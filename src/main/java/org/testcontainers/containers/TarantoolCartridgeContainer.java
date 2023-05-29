package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;

import org.testcontainers.containers.exceptions.CartridgeTopologyException;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

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
 * @authorm Artyom Dubinin
 * @author Ivan Dneprov
 *
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
    private static final String ENV_TARANTOOL_INSTANCES_FILE = "TARANTOOL_INSTANCES_FILE";

    private final CartridgeConfigParser instanceFileParser;
    private final TarantoolContainerClientHelper clientHelper;
    private final String TARANTOOL_RUN_DIR;

    private boolean useFixedPorts = false;
    private String routerHost = ROUTER_HOST;
    private int routerPort = ROUTER_PORT;
    private int apiPort = API_PORT;
    private String routerUsername = CARTRIDGE_USERNAME;
    private String routerPassword = CARTRIDGE_PASSWORD;
    private String directoryResourcePath = SCRIPT_RESOURCE_DIRECTORY;
    private String instanceDir = INSTANCE_DIR;
    private String topologyConfigurationFile;
    private Boolean sslIsActive = false;
    private String keyFile = "";
    private String certFile = "";

    /**
     * Create a container with default image and specified instances file from the classpath resources. Assumes that
     * there is a file named Dockerfile in the project resources classpath.
     *
     * @param instancesFile             path to instances.yml, relative to the classpath resources
     * @param topologyConfigurationFile path to a topology bootstrap script, relative to the classpath resources
     */
    public TarantoolCartridgeContainer(String instancesFile, String topologyConfigurationFile) {
        this(DOCKERFILE, instancesFile, topologyConfigurationFile);
    }

    /**
     * Create a container with default image and specified instances file from the classpath resources. Assumes that
     * there is a file named Dockerfile in the project resources classpath.
     *
     * @param instancesFile             path to instances.yml, relative to the classpath resources
     * @param topologyConfigurationFile path to a topology bootstrap script, relative to the classpath resources
     * @param buildArgs                 a map of arguments that will be passed to docker ARG commands on image build.
     *                                  This values can be overridden by environment.
     */
    public TarantoolCartridgeContainer(String instancesFile, String topologyConfigurationFile,
                                       Map<String, String> buildArgs) {
        this(DOCKERFILE, "", instancesFile, topologyConfigurationFile, buildArgs);
    }

    /**
     * Create a container with default image and specified instances file from the classpath resources
     *
     * @param dockerFile                path to a Dockerfile which configures Cartridge and other necessary services
     * @param instancesFile             path to instances.yml, relative to the classpath resources
     * @param topologyConfigurationFile path to a topology bootstrap script, relative to the classpath resources
     */
    public TarantoolCartridgeContainer(String dockerFile, String instancesFile, String topologyConfigurationFile) {
        this(dockerFile, "", instancesFile, topologyConfigurationFile);
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
        this(dockerFile, buildImageName, instancesFile, topologyConfigurationFile, Collections.emptyMap());
    }

    /**
     * Create a container with specified image and specified instances file from the classpath resources. By providing
     * the result Cartridge container image name, you can cache the image and avoid rebuilding on each test run (the
     * image is tagged with the provided name and not deleted after tests finishing).
     *
     * @param dockerFile                URL resource path to a Dockerfile which configures Cartridge
     *                                  and other necessary services
     * @param buildImageName            Specify a stable image name for the test container to prevent rebuilds
     * @param instancesFile             URL resource path to instances.yml relative in the classpath
     * @param topologyConfigurationFile URL resource path to a topology bootstrap script in the classpath
     * @param buildArgs                 a map of arguments that will be passed to docker ARG commands on image build.
     *                                  This values can be overridden by environment.
     */
    public TarantoolCartridgeContainer(String dockerFile, String buildImageName, String instancesFile,
                                       String topologyConfigurationFile, final Map<String, String> buildArgs) {
        this(buildImage(dockerFile, buildImageName), instancesFile, topologyConfigurationFile, buildArgs);
    }

    private TarantoolCartridgeContainer(ImageFromDockerfile image, String instancesFile, String topologyConfigurationFile,
                                        Map<String, String> buildArgs) {
        super(withBuildArgs(image, buildArgs));

        TARANTOOL_RUN_DIR = mergeBuildArguments(buildArgs).getOrDefault(ENV_TARANTOOL_RUNDIR, "/tmp/run");

        if (instancesFile == null || instancesFile.isEmpty()) {
            throw new IllegalArgumentException("Instance file name must not be null or empty");
        }
        if (topologyConfigurationFile == null || topologyConfigurationFile.isEmpty()) {
            throw new IllegalArgumentException("Topology configuration file must not be null or empty");
        }
        this.topologyConfigurationFile = topologyConfigurationFile;
        this.instanceFileParser = new CartridgeConfigParser(instancesFile);
        this.clientHelper = new TarantoolContainerClientHelper(this);
    }

    private static ImageFromDockerfile withBuildArgs(ImageFromDockerfile image, Map<String, String> buildArgs) {
        Map<String, String> args = mergeBuildArguments(buildArgs);

        if (!args.isEmpty()) {
            image.withBuildArgs(args);
        }

        return image;
    }

    private static Map<String, String> mergeBuildArguments(Map<String, String> buildArgs) {
        Map<String, String> args = new HashMap<>(buildArgs);

        for (String envVariable : Arrays.asList(
                ENV_TARANTOOL_VERSION,
                ENV_TARANTOOL_SERVER_USER,
                ENV_TARANTOOL_SERVER_UID,
                ENV_TARANTOOL_SERVER_GROUP,
                ENV_TARANTOOL_SERVER_GID,
                ENV_TARANTOOL_WORKDIR,
                ENV_TARANTOOL_RUNDIR,
                ENV_TARANTOOL_DATADIR,
                ENV_TARANTOOL_INSTANCES_FILE
        )) {
            String variableValue = System.getenv(envVariable);
            if (variableValue != null && !args.containsKey(envVariable)) {
                args.put(envVariable, variableValue);
            }
        }
        return args;
    }

    private static ImageFromDockerfile buildImage(String dockerFile, String buildImageName) {
        if (buildImageName != null && !buildImageName.isEmpty()) {
            return new ImageFromDockerfile(buildImageName, false)
                    .withFileFromClasspath("Dockerfile", dockerFile);
        }
        return new ImageFromDockerfile().withFileFromClasspath("Dockerfile", dockerFile);
    }

    /**
     * Specify SSL as connection transport.
     * Warning! SSL must be set as default transport on your tarantool cluster.
     * Supported only in Tarantool Enterprise
     *
     * @return this container instance
     */
    public TarantoolCartridgeContainer withSsl() {
        checkNotRunning();
        this.sslIsActive = true;
        return this;
    }

    /**
     * Specify path to key and cert files inside your container for SSL connection.
     * Warning! SSL must be set as default transport on your tarantool cluster.
     * Supported only in Tarantool Enterprise
     *
     * @return this container instance
     */
    public TarantoolCartridgeContainer withKeyAndCertFiles(String keyFile, String certFile) {
        checkNotRunning();
        this.keyFile = keyFile;
        this.certFile = certFile;
        return this;
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
        if (useFixedPorts) {
            return routerPort;
        }
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

    @Override
    public int getInternalPort() {
        return routerPort;
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
        if (useFixedPorts) {
            return apiPort;
        }
        return getMappedPort(apiPort);
    }

    /**
     * Use fixed ports binding.
     * Defaults to false.
     *
     * @param useFixedPorts fixed ports for tarantool
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

    private boolean setupTopology() {
        String fileType = topologyConfigurationFile.substring(topologyConfigurationFile.lastIndexOf('.') + 1);

        if (fileType.equals("yml")) {
            String replicasetsFileName = topologyConfigurationFile
                    .substring(topologyConfigurationFile.lastIndexOf('/') + 1);

            try {
                Container.ExecResult result = execInContainer("cartridge",
                        "replicasets",
                        "--run-dir=" + TARANTOOL_RUN_DIR,
                        "--file=" + replicasetsFileName, "setup", "--bootstrap-vshard");
                if (result.getExitCode() != 0) {
                    throw new CartridgeTopologyException("Failed to change the app topology via cartridge CLI: "
                                                             + result.getStdout());
                }
            } catch (Exception e) {
                throw new CartridgeTopologyException(e);
            }

        } else {
            try {
                ArrayList<?> res = executeScriptDecoded(topologyConfigurationFile);
                if (res.size() >= 2 && res.get(1) != null && res.get(1) instanceof Map) {
                    HashMap<?, ?> error = ((HashMap<?, ?>) res.get(1));
                    // that means topology already exists
                    return error.get("str").toString().contains("collision with another server");
                }
                // The client connection will be closed after that command
            } catch (Exception e) {
                if (e instanceof ExecutionException) {
                    if (e.getCause() instanceof TimeoutException) {
                        return true;
                        // Do nothing, the cluster is reloading
                    }
                } else {
                    throw new CartridgeTopologyException(e);
                }
            }
        }
        return true;
    }

    private void retryingSetupTopology() {
        if (!setupTopology()) {
            try {
                logger().info("Retrying setup topology in 10 seconds");
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (!setupTopology()) {
                throw new CartridgeTopologyException("Failed to change the app topology after retry");
            }
        }
    }

    private void bootstrapVshard() {
        try {
            executeCommand(VSHARD_BOOTSTRAP_COMMAND);
        } catch (Exception e) {
            logger().error("Failed to bootstrap vshard cluster", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo, boolean reused) {
        super.containerIsStarted(containerInfo, reused);

        waitUntilRouterIsUp(60);
        retryingSetupTopology();
        // wait until Roles are configured
        waitUntilCartridgeIsHealthy(10);
        bootstrapVshard();

        logger().info("Tarantool Cartridge cluster is started");
        logger().info("Tarantool Cartridge router is listening at {}:{}", getRouterHost(), getRouterPort());
        logger().info("Tarantool Cartridge HTTP API is available at {}:{}", getAPIHost(), getAPIPort());
    }

    private void waitUntilRouterIsUp(int secondsToWait) {
        waitUntilTrue(secondsToWait, this::routerIsUp);
    }

    private void waitUntilCartridgeIsHealthy(int secondsToWait) {
        waitUntilTrue(secondsToWait, this::isCartridgeHealthy);
    }

    private void waitUntilTrue(int secondsToWait, Supplier<Boolean> waitFunc) {
        int secondsPassed = 0;
        boolean result = waitFunc.get();
        while (!result && secondsPassed < secondsToWait) {
            result = waitFunc.get();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
        if (!result) {
            throw new RuntimeException("Failed to change the app topology after retry");
        }
    }

    private boolean routerIsUp() {
        String healthyCmd = " local cartridge = package.loaded['cartridge']" +
                " return assert(cartridge ~= nil)";
        try {
            Container.ExecResult result = executeCommand(healthyCmd);
            return result.getStdout().equals("---\n- true\n...\n\n");
        } catch (Exception e) {
            logger().warn("Error while waiting for router instance to be up: " + e.getMessage());
            return false;
        }
    }

    private boolean isCartridgeHealthy() {
        String healthyCmd = " local cartridge = package.loaded['cartridge']" +
                " return assert(cartridge) and assert(cartridge.is_healthy())";
        try {
            Container.ExecResult result = executeCommand(healthyCmd);
            return result.getStdout().equals("---\n- true\n...\n\n");
        } catch (Exception e) {
            logger().warn("Error while waiting for cartridge healthy state: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Container.ExecResult executeScript(String scriptResourcePath) throws Exception {
        return clientHelper.executeScript(scriptResourcePath, this.sslIsActive, this.keyFile, this.certFile);
    }

    @Override
    public <T> T executeScriptDecoded(String scriptResourcePath) throws Exception {
        return clientHelper.executeScriptDecoded(scriptResourcePath, this.sslIsActive, this.keyFile, this.certFile);
    }

    @Override
    public Container.ExecResult executeCommand(String command) throws Exception {
        return clientHelper.executeCommand(command, this.sslIsActive, this.keyFile, this.certFile);
    }

    @Override
    public <T> T executeCommandDecoded(String command) throws Exception {
        return clientHelper.executeCommandDecoded(command, this.sslIsActive, this.keyFile, this.certFile);
    }
}
