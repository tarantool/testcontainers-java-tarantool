package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import io.tarantool.driver.api.TarantoolClient;
import io.tarantool.driver.TarantoolClientConfig;
import io.tarantool.driver.TarantoolServerAddress;
import io.tarantool.driver.auth.SimpleTarantoolCredentials;
import io.tarantool.driver.auth.TarantoolCredentials;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.images.builder.dockerfile.DockerfileBuilder;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

/**
 * Sets up a Tarantool Cartridge cluster and provides API for configuring it.
 *
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
 *
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
 *
 * After the topology changes are applied, the vshard bootstrap command will be executed.
 *
 * The instances.yml file will be analyzed and the ports, specified in advertise_uri options together with the ports,
 * specified in the http_port options, will be exposed.
 *
 * @author Alexey Kuzin
 */
public class TarantoolCartridgeContainer<SELF extends TarantoolCartridgeContainer<SELF>>
        extends TarantoolContainer<SELF> implements Container<SELF> {

    private static final String ROUTER_HOST = "localhost";
    private static final int ROUTER_PORT = 3301;
    private static final String CARTRIDGE_USERNAME = "admin";
    private static final String CARTRIDGE_PASSWORD = "testapp-cluster-cookie";
    private static final String DOCKERFILE_USE_ROOT_PROPERTY = "dockerfileUseRoot";
    private static final int API_PORT = 8081;
    private static final String VSHARD_BOOTSTRAP_COMMAND = "return require('cartridge').admin_bootstrap_vshard()";

    private String routerHost = ROUTER_HOST;
    private int routerPort = ROUTER_PORT;
    private int apiPort = API_PORT;
    private String routerUsername = CARTRIDGE_USERNAME;
    private String routerPassword = CARTRIDGE_PASSWORD;
    private final String instancesFile;
    private final CartridgeConfigParser instanceFileParser;
    private final String topologyConfigurationFile;

    /**
     * Create a container with default image and specified instances file from the classpath resources
     *
     * @param instancesFile path to instances.yml, relative to the classpath resources
     * @param topologyConfigurationFile path to a topology bootstrap script, relative to the classpath resources
     */
    public TarantoolCartridgeContainer(String instancesFile, String topologyConfigurationFile) {
        this(DEFAULT_TARANTOOL_BASE_IMAGE, instancesFile, topologyConfigurationFile);
    }

    /**
     * Create a container with specified image and specified instances file from the classpath resources
     *
     * @param dockerImageName Tarantool docker image name with version
     * @param instancesFile path to instances.yml relative to the classpath resources
     * @param topologyConfigurationFile path to a topology bootstrap script, relative to the classpath resources
     */
    public TarantoolCartridgeContainer(String dockerImageName, String instancesFile, String topologyConfigurationFile) {
        super(buildImage(dockerImageName));
        if (instancesFile == null || instancesFile.isEmpty()) {
            throw new IllegalArgumentException("Instance file name must not be null or empty");
        }
        this.instancesFile = instancesFile;
        this.instanceFileParser = new CartridgeConfigParser(instancesFile);
        this.topologyConfigurationFile = topologyConfigurationFile;
    }

    /**
     * Create a container with specified image and specified instances file from the classpath resources. By providing
     * the result Cartridge container image name, you can cache the image and avoid rebuilding on each test run (the
     * image is tagged with the provided name and not deleted after tests finishing).
     *
     * @param dockerImageName Tarantool docker image name with version
     * @param buildImageName Specify a stable image name for the test container to prevent rebuilds
     * @param instancesFile path to instances.yml relative to the classpath resources
     * @param topologyConfigurationFile path to a topology bootstrap script, relative to the classpath resources
     */
    public TarantoolCartridgeContainer(String dockerImageName, String buildImageName,
                                       String instancesFile, String topologyConfigurationFile) {
        super(buildImage(dockerImageName, buildImageName));
        if (instancesFile == null || instancesFile.isEmpty()) {
            throw new IllegalArgumentException("Instance file name must not be null or empty");
        }
        this.instancesFile = instancesFile;
        this.instanceFileParser = new CartridgeConfigParser(instancesFile);
        this.topologyConfigurationFile = topologyConfigurationFile;
    }

    private static Future<String> buildImage(String baseImageName) {
        return new ImageFromDockerfile()
                .withDockerfileFromBuilder(builder -> makeDockerfile(builder, baseImageName));
    }

    private static Future<String> buildImage(String baseImageName, String buildImageName) {
        return new ImageFromDockerfile(buildImageName, false)
                .withDockerfileFromBuilder(builder -> makeDockerfile(builder, baseImageName));
    }

    private static String makeDockerfile(DockerfileBuilder builder, String baseImageName) {
        String useRoot = System.getProperty("useRoot");
        builder
            .from(baseImageName)
            .run("/bin/sh", "-c",
                    "curl -L https://tarantool.io/installer.sh | VER=2.4 /bin/bash -s -- --repo-only && " +
                            "yum -y install cmake make gcc git cartridge-cli && cartridge version")
            .workDir(INSTANCE_DIR)
            .cmd("cartridge build && cartridge start");
        if (!Boolean.parseBoolean(System.getProperty(DOCKERFILE_USE_ROOT_PROPERTY))) {
            String userGroup = String.format("%s:%s", TARANTOOL_SERVER_USER, TARANTOOL_SERVER_GROUP);
            builder.user(userGroup);
        }
        return builder.build();
    }

    protected TarantoolClient getRouterClient() {
        TarantoolCredentials credentials = new SimpleTarantoolCredentials(getUsername(), getPassword());
        TarantoolServerAddress address = new TarantoolServerAddress(getRouterHost(), getRouterPort());
        TarantoolClientConfig config = TarantoolClientConfig.builder().withCredentials(credentials).build();
        return getClient(config, address);
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

    /**
     * Get Cartridge router HTTP API hostname
     *
     * @return HTTP API hostname
     */
    public String getAPIHost() {
        return routerHost;
    }

    @Override
    public TarantoolCartridgeContainer<SELF> withDirectoryBinding(String directoryResourcePath) {
        super.withDirectoryBinding(directoryResourcePath);
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
     * Set Cartridge router hostname
     *
     * @param routerHost a hostname, default is "localhost"
     * @return this container instance
     */
    public TarantoolCartridgeContainer<SELF> withRouterHost(String routerHost) {
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
    public TarantoolCartridgeContainer<SELF> withRouterPort(int routerPort) {
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
    public TarantoolCartridgeContainer<SELF> withAPIPort(int apiPort) {
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
    public TarantoolCartridgeContainer<SELF> withRouterUsername(String routerUsername) {
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
    public TarantoolCartridgeContainer<SELF> withRouterPassword(String routerPassword) {
        checkNotRunning();
        this.routerPassword = routerPassword;
        return this;
    }

    @Override
    public TarantoolCartridgeContainer<SELF> withLogLevel(TarantoolLogLevel logLevel) {
        // not supported
        return this;
    }

    @Override
    public TarantoolCartridgeContainer<SELF> withMemtxMemory(Integer memtxMemory) {
        // not supported
        return this;
    }

    @Override
    protected void configure() {
        withFileSystemBind(getDirectoryBinding(), getInstanceDir(), BindMode.READ_WRITE);
        withExposedPorts(instanceFileParser.getExposablePorts());
        withUsername(getRouterUsername());
        withPassword(getRouterPassword());
        withEnv("BOOTSTRAP", "ON");
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
    public TarantoolCartridgeContainer<SELF> cleanUpDirectory(String directoryResourcePath) {
        super.cleanUpDirectory(directoryResourcePath);
        return this;
    }
}
