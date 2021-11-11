package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import static org.testcontainers.containers.PathUtils.normalizePath;

/**
 * Sets up a Tarantool Cartridge cluster and provides API for configuring it.
 * <p>
 * The container constructors accept the classpath resources relative path to the instances.yml file, which contents
 * may look like
 *
 * <pre>
 * <code>
 * testapp.router:
 *   workdir: ./tmp/db_dev/3301
 *   advertise_uri: localhost:3301
 *   http_port: 8081
 *
 * testapp.s1-master:
 *   workdir: ./tmp/db_dev/3302
 *   advertise_uri: localhost:3302
 *   http_port: 8082
 *
 * testapp.s1-replica:
 *   workdir: ./tmp/db_dev/3303
 *   advertise_uri: localhost:3303
 *   http_port: 8083
 *
 * testapp.s2-master:
 *   workdir: ./tmp/db_dev/3304
 *   advertise_uri: localhost:3304
 *   http_port: 8084
 *
 * testapp.s2-replica:
 *   workdir: ./tmp/db_dev/3305
 *   advertise_uri: localhost:3305
 *   http_port: 8085
 * </code>
 * </pre>
 * <p>
 * and the classpath resources relative path to a topology bootstrap script, which contents may look like
 *
 * <pre>
 * <code>
 * cartridge = require('cartridge')
 * replicasets = {{
 *     alias = 'app-router',
 *     roles = {'vshard-router', 'app.roles.custom', 'app.roles.api_router'},
 *     join_servers = {{uri = 'localhost:3301'}}
 * }, {
 *     alias = 's1-storage',
 *     roles = {'vshard-storage', 'app.roles.api_storage'},
 *     join_servers = {{uri = 'localhost:3302'}, {uri = 'localhost:3303'}}
 * }, {
 *     alias = 's2-storage',
 *     roles = {'vshard-storage', 'app.roles.api_storage'},
 *     join_servers = {{uri = 'localhost:3304'}, {uri = 'localhost:3305'}}
 * }}
 * return cartridge.admin_edit_topology({replicasets = replicasets})
 * </code>
 * </pre>
 * <p>
 * After the topology changes are applied, the vshard bootstrap command will be executed.
 * <p>
 * The instances.yml file will be analyzed and the ports, specified in advertise_uri options together with the ports,
 * specified in the http_port options, will be exposed.
 *
 * @author Alexey Kuzin
 */
public class TarantoolCartridgeContainer extends GenericContainer<TarantoolCartridgeContainer>
        implements TarantoolContainerOperations<TarantoolCartridgeContainer> {

    private static final String ROUTER_HOST = "localhost";
    private static final int ROUTER_PORT = 3301;
    private static final String CARTRIDGE_USERNAME = "admin";
    private static final String CARTRIDGE_PASSWORD = "testapp-cluster-cookie";
    private static final String DOCKERFILE = "Dockerfile";
    private static final int API_PORT = 8081;
    private static final String VSHARD_BOOTSTRAP_COMMAND = "return require('cartridge').admin_bootstrap_vshard()";
    private static final String SCRIPT_RESOURCE_DIRECTORY = "";
    private static final String INSTANCE_DIR = "/app";

    private static final String ENV_TARANTOOL_VERSION = "TARANTOOL_VERSION";
    private static final String ENV_TARANTOOL_SERVER_USER = "TARANTOOL_SERVER_USER";
    private static final String ENV_TARANTOOL_SERVER_UID = "TARANTOOL_SERVER_UID";
    private static final String ENV_TARANTOOL_SERVER_GROUP = "TARANTOOL_SERVER_GROUP";
    private static final String ENV_TARANTOOL_SERVER_GID = "TARANTOOL_SERVER_GID";
    private static final String ENV_TARANTOOL_WORKDIR = "TARANTOOL_WORKDIR";
    private static final String ENV_TARANTOOL_RUNDIR = "TARANTOOL_RUNDIR";
    private static final String ENV_TARANTOOL_DATADIR = "TARANTOOL_DATADIR";
    private boolean useFixedPorts = false;

    private String routerHost = ROUTER_HOST;
    private int routerPort = ROUTER_PORT;
    private int apiPort = API_PORT;
    private String routerUsername = CARTRIDGE_USERNAME;
    private String routerPassword = CARTRIDGE_PASSWORD;
    private String directoryResourcePath = SCRIPT_RESOURCE_DIRECTORY;
    private String instanceDir = INSTANCE_DIR;
    private final String instancesFile;
    private final CartridgeConfigParser instanceFileParser;
    private final String topologyConfigurationFile;
    private final TarantoolContainerClientHelper clientHelper;

    /**
     * Create a container with default image and specified instances file from the classpath resources. Assumes that
     * there is a file named Dockerfile in the project resources classpath.
     *
     * @param instancesFile             path to instances.yml, relative to the classpath resources
     * @param topologyConfigurationFile path to a topology bootstrap script, relative to the classpath resources
     */
    public TarantoolCartridgeContainer(String instancesFile, String topologyConfigurationFile) {
        this(withArguments(buildImage()), instancesFile, topologyConfigurationFile);
    }

    /**
     * Create a container with default image and specified instances file from the classpath resources
     *
     * @param dockerFile                path to a Dockerfile which configures Cartridge and other necessary services
     * @param instancesFile             path to instances.yml, relative to the classpath resources
     * @param topologyConfigurationFile path to a topology bootstrap script, relative to the classpath resources
     */
    public TarantoolCartridgeContainer(String dockerFile, String instancesFile, String topologyConfigurationFile) {
        this(withArguments(buildImage(dockerFile)), instancesFile, topologyConfigurationFile);
    }

    /**
     * Create a container with specified image and specified instances file from the classpath resources. By providing
     * the result Cartridge container image name, you can cache the image and avoid rebuilding on each test run (the
     * image is tagged with the provided name and not deleted after tests finishing).
     *
     * @param dockerFile                URL resource path to a Dockerfile which configures Cartridge and other necessary services
     * @param buildImageName            Specify a stable image name for the test container to prevent rebuilds
     * @param instancesFile             URL resource path to instances.yml relative in the classpath
     * @param topologyConfigurationFile URL resource path to a topology bootstrap script in the classpath
     */
    public TarantoolCartridgeContainer(String dockerFile, String buildImageName,
                                       String instancesFile, String topologyConfigurationFile) {
        this(withArguments(buildImage(dockerFile, buildImageName)), instancesFile, topologyConfigurationFile);
    }

    private TarantoolCartridgeContainer(Future<String> image, String instancesFile, String topologyConfigurationFile) {
        super(image);
        if (instancesFile == null || instancesFile.isEmpty()) {
            throw new IllegalArgumentException("Instance file name must not be null or empty");
        }
        this.instancesFile = instancesFile;
        this.instanceFileParser = new CartridgeConfigParser(instancesFile);
        this.topologyConfigurationFile = topologyConfigurationFile;
        this.clientHelper = new TarantoolContainerClientHelper(this);
    }

    private static Future<String> withArguments(ImageFromDockerfile image) {
        for (String envVariable : Arrays.asList(
                ENV_TARANTOOL_VERSION,
                ENV_TARANTOOL_SERVER_USER,
                ENV_TARANTOOL_SERVER_UID,
                ENV_TARANTOOL_SERVER_GROUP,
                ENV_TARANTOOL_SERVER_GID,
                ENV_TARANTOOL_WORKDIR,
                ENV_TARANTOOL_RUNDIR,
                ENV_TARANTOOL_DATADIR
        )) {
            String variableValue = System.getenv(envVariable);
            if (variableValue != null) {
                image.withBuildArg(envVariable, variableValue);
            }
        }
        return image;
    }

    private static ImageFromDockerfile buildImage() {
        return buildImage(DOCKERFILE);
    }

    private static ImageFromDockerfile buildImage(String dockerFile) {
        return new ImageFromDockerfile().withFileFromClasspath("Dockerfile", dockerFile);
    }

    private static ImageFromDockerfile buildImage(String dockerFile, String buildImageName) {
        return new ImageFromDockerfile(buildImageName, false)
                .withFileFromClasspath("Dockerfile", dockerFile);
    }

    /**
     * Get the router host
     *
     * @return router hostname
     */
    public String getRouterHost() {
        return routerHost;
    }

    /**
     * Get the router port
     *
     * @return router mapped port
     */
    public int getRouterPort() {
        return getMappedPort(routerPort);
    }

    /**
     * Get the user name for connecting to the router
     *
     * @return a user name
     */
    public String getRouterUsername() {
        return routerUsername;
    }

    /**
     * Get the user password for connecting to the router
     *
     * @return a user password
     */
    public String getRouterPassword() {
        return routerPassword;
    }

    @Override
    public String getHost() {
        return getRouterHost();
    }

    @Override
    public int getPort() {
        return getRouterPort();
    }

    @Override
    public String getUsername() {
        return getRouterUsername();
    }

    @Override
    public String getPassword() {
        return getRouterPassword();
    }

    @Override
    public String getDirectoryBinding() {
        return directoryResourcePath;
    }

    /**
     * Specify the directory inside container that the resource directory will be mounted to. The default value is
     * "/app".
     *
     * @param instanceDir valid directory path
     * @return this container instance
     */
    public TarantoolCartridgeContainer withInstanceDir(String instanceDir) {
        checkNotRunning();
        this.instanceDir = instanceDir;
        return this;
    }

    @Override
    public String getInstanceDir() {
        return instanceDir;
    }

    /**
     * Get Cartridge router HTTP API hostname
     *
     * @return HTTP API hostname
     */
    public String getAPIHost() {
        return routerHost;
    }

    /**
     * Checks if already running and if so raises an exception to prevent too-late setters.
     */
    protected void checkNotRunning() {
        if (isRunning()) {
            throw new IllegalStateException("This option can be changed only before the container is running");
        }
    }

    /**
     * Specify the root directory of a Cartridge project relative to the resource classpath.
     * The default directory is the root resource directory.
     *
     * @param directoryResourcePath a valid directory path
     * @return this container instance
     */
    public TarantoolCartridgeContainer withDirectoryBinding(String directoryResourcePath) {
        checkNotRunning();
        URL resource = getClass().getClassLoader().getResource(directoryResourcePath);
        if (resource == null) {
            throw new IllegalArgumentException(
                    String.format("No resource path found for the specified resource %s", directoryResourcePath));
        }
        this.directoryResourcePath = normalizePath(resource.getPath());
        return this;
    }

    /**
     * Get Cartridge router HTTP API port
     *
     * @return HTTP API port
     */
    public int getAPIPort() {
        return getMappedPort(apiPort);
    }

    /**
     * Use fixed ports binding.
     * Defaults to false.
     *
     * @return HTTP API port
     */
    public TarantoolCartridgeContainer withUseFixedPorts(boolean useFixedPorts) {
        this.useFixedPorts = useFixedPorts;
        return this;
    }

    /**
     * Set Cartridge router hostname
     *
     * @param routerHost a hostname, default is "localhost"
     * @return this container instance
     */
    public TarantoolCartridgeContainer withRouterHost(String routerHost) {
        checkNotRunning();
        this.routerHost = routerHost;
        return this;
    }

    /**
     * Set Cartridge router binary port
     *
     * @param routerPort router Tarantool node port, usually 3301
     * @return this container instance
     */
    public TarantoolCartridgeContainer withRouterPort(int routerPort) {
        checkNotRunning();
        this.routerPort = routerPort;
        return this;
    }

    /**
     * Set Cartridge router HTTP API port
     *
     * @param apiPort HTTP API port, usually 8081
     * @return this container instance
     */
    public TarantoolCartridgeContainer withAPIPort(int apiPort) {
        checkNotRunning();
        this.apiPort = apiPort;
        return this;
    }

    /**
     * Set the username for accessing the router node
     *
     * @param routerUsername a user name, default is "admin"
     * @return this container instance
     */
    public TarantoolCartridgeContainer withRouterUsername(String routerUsername) {
        checkNotRunning();
        this.routerUsername = routerUsername;
        return this;
    }

    /**
     * Set the user password for accessing the router node
     *
     * @param routerPassword a user password, usually is a value of the "cluster_cookie" option in cartridge.cfg({...})
     * @return this container instance
     */
    public TarantoolCartridgeContainer withRouterPassword(String routerPassword) {
        checkNotRunning();
        this.routerPassword = routerPassword;
        return this;
    }

    @Override
    protected void configure() {
        if (!getDirectoryBinding().isEmpty()) {
            withFileSystemBind(getDirectoryBinding(), getInstanceDir(), BindMode.READ_WRITE);
        }

        if (useFixedPorts) {
            for (Integer port : instanceFileParser.getExposablePorts()) {
                addFixedExposedPort(port, port);
            }
        } else {
            withExposedPorts(instanceFileParser.getExposablePorts());
        }
    }

    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo) {
        logger().info("Tarantool Cartridge cluster is starting");
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo, boolean reused) {
        super.containerIsStarted(containerInfo, reused);

        try {
            executeScript(topologyConfigurationFile).get();
            // The client connection will be closed after that command
        } catch (Exception e) {
            if (e instanceof ExecutionException && e.getCause() instanceof TimeoutException) {
                // Do nothing, the cluster is reloading
            } else {
                logger().error("Failed to change the app topology", e);
                throw new RuntimeException(e);
            }
        }

        // The client must reconnect automatically
        try {
            executeCommand(VSHARD_BOOTSTRAP_COMMAND).get();
        } catch (Exception e) {
            logger().error("Failed to bootstrap vshard cluster", e);
            throw new RuntimeException(e);
        }

        logger().info("Tarantool Cartridge cluster is started");
        logger().info("Tarantool Cartridge router is listening at {}:{}", getRouterHost(), getRouterPort());
        logger().info("Tarantool Cartridge HTTP API is available at {}:{}", getAPIHost(), getAPIPort());
    }

    @Override
    public CompletableFuture<List<?>> executeScript(String scriptResourcePath) throws Exception {
        return clientHelper.executeScript(scriptResourcePath);
    }

    @Override
    public CompletableFuture<List<?>> executeCommand(String command, Object... arguments) throws Exception {
        return clientHelper.executeCommand(command, arguments);
    }
}
