package org.testcontainers.containers;

import java.net.URL;
import java.nio.file.Paths;
import java.util.concurrent.Future;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.containers.wait.strategy.Wait;
import static org.testcontainers.containers.PathUtils.normalizePath;

/**
 * Sets up a Tarantool instance and provides API for configuring it.
 *
 * @author Alexey Kuzin
 * @author Ivan Dneprov
 */
public class TarantoolContainer extends GenericContainer<TarantoolContainer>
        implements TarantoolContainerOperations<TarantoolContainer> {

    public static final String TARANTOOL_IMAGE = "tarantool/tarantool";
    public static final String DEFAULT_IMAGE_VERSION = "2.x";
    public static final String DEFAULT_TARANTOOL_BASE_IMAGE;


    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 3301;
    private static final String API_USER = "api_user";
    private static final String API_PASSWORD = "secret";
    private static final TarantoolLogLevel LOG_LEVEL = TarantoolLogLevel.VERBOSE;
    private static final Integer MEMTX_MEMORY = 128 * 1024 * 1024; // 128 Mb in bytes
    private static final String SCRIPT_RESOURCE_DIRECTORY = "";
    private static final String SCRIPT_FILENAME = "server.lua";
    private static final String INSTANCE_DIR = "/app";

    private String username = API_USER;
    private String password = API_PASSWORD;
    private String host = DEFAULT_HOST;
    private Integer port = DEFAULT_PORT;
    private TarantoolLogLevel logLevel = LOG_LEVEL;
    private Integer memtxMemory = MEMTX_MEMORY;
    private String directoryResourcePath = SCRIPT_RESOURCE_DIRECTORY;
    private String scriptFileName = SCRIPT_FILENAME;
    private String instanceDir = INSTANCE_DIR;
    private boolean useFixedPorts = false;
    private SslContext sslContext;

    private final TarantoolContainerClientHelper clientHelper;

    static {
        String version = System.getenv("TARANTOOL_VERSION");
        if (version == null || version.trim().isEmpty()) {
            DEFAULT_TARANTOOL_BASE_IMAGE = String.format("%s:%s-centos7", TARANTOOL_IMAGE, DEFAULT_IMAGE_VERSION);
        } else {
            DEFAULT_TARANTOOL_BASE_IMAGE = String.format("%s:%s-centos7", TARANTOOL_IMAGE, version);
        }
    }

    /**
     * Constructor for {@link TarantoolContainer}
     */
    public TarantoolContainer() {
        this(DEFAULT_TARANTOOL_BASE_IMAGE);
    }

    /**
     * Constructor for {@link TarantoolContainer}
     *
     * @param dockerImageName docker image name for container creating
     */
    public TarantoolContainer(String dockerImageName) {
        super(dockerImageName);
        clientHelper = new TarantoolContainerClientHelper(this);
    }

    /**
     * Constructor for {@link TarantoolContainer}
     *
     * @param tarantoolImageParams params for cached image creating
     */
    public TarantoolContainer(TarantoolImageParams tarantoolImageParams) {
        super(TarantoolContainerImageHelper.getImage(tarantoolImageParams));
        clientHelper = new TarantoolContainerClientHelper(this);
    }

    /**
     * Constructor for {@link TarantoolContainer}
     *
     * @param image future with image name
     */
    public TarantoolContainer(Future<String> image) {
        super(image);
        clientHelper = new TarantoolContainerClientHelper(this);
    }

    /**
     * Use fixed ports binding.
     * Defaults to false.
     *
     * @param useFixedPorts fixed ports for tarantool
     * @return HTTP API port
     */
    public TarantoolContainer withUseFixedPorts(boolean useFixedPorts) {
        this.useFixedPorts = useFixedPorts;
        return this;
    }

    /**
     * Specify the host for connecting to Tarantool with.
     *
     * @param host valid IP address or hostname
     * @return this container instance
     */
    public TarantoolContainer withHost(String host) {
        checkNotRunning();
        this.host = host;
        return this;
    }

    /**
     * Specify the port for connecting to Tarantool with.
     *
     * @param port valid port number
     * @return this container instance
     */
    public TarantoolContainer withPort(int port) {
        checkNotRunning();
        this.port = port;
        return this;
    }

    public TarantoolContainer withFixedExposedPort(int hostPort, int containerPort) {
        super.addFixedExposedPort(hostPort, containerPort);
        return this;
    }

    public TarantoolContainer withExposedPort(Integer port) {
        super.addExposedPort(port);
        return this;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return getMappedPort(port);
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Specify the username for connecting to Tarantool with.
     * Warning! This user must be created on Tarantool instance startup, e.g. specified in the startup script.
     *
     * @param username the client user name
     * @return this container instance
     */
    public TarantoolContainer withUsername(String username) {
        checkNotRunning();
        this.username = username;
        return this;
    }

    /**
     * Specify the password for the specified user for connecting to Tarantool with.
     * Warning! This user must be created on Tarantool instance startup, e.g. specified in the startup script,
     * together with setting the password.
     *
     * @param password the client user password
     * @return this container instance
     */
    public TarantoolContainer withPassword(String password) {
        checkNotRunning();
        this.password = password;
        return this;
    }


    /**
     * Specify SSL as connection transport and path to key and cert files inside your container for mTLS connection.
     * Warning! SSL must be set as the default transport in your Tarantool cluster.
     * Supported only in Tarantool Enterprise.
     *
     * @param sslContext {@link SslContext} instance
     * @return this container instance
     */
    public TarantoolContainer withSslContext(SslContext sslContext) {
        checkNotRunning();
        this.sslContext = sslContext;
        return this;
    }

    /**
     * Change the log_level setting on the Tarantool instance
     *
     * @param logLevel new log_level value
     * @return this container instance
     */
    public TarantoolContainer withLogLevel(TarantoolLogLevel logLevel) {
        this.logLevel = logLevel;
        if (isRunning()) {
            try {
                executeCommand(logLevel.toCommand());
            } catch (Exception e) {
                logger().error(String.format("Failed to set log_level to %s", logLevel.toString()), e);
                throw new RuntimeException(e);
            }
        }
        return this;
    }

    /**
     * Change the memtx_memory setting on the Tarantool instance
     *
     * @param memtxMemory new memtx_memory value, must be greater than 0
     * @return this container instance
     */
    public TarantoolContainer withMemtxMemory(Integer memtxMemory) {
        if (memtxMemory <= 0) {
            throw new RuntimeException(
                    String.format("The specified memtx_memory value must be >= 0, but was %d", memtxMemory));
        }
        this.memtxMemory = memtxMemory;
        if (isRunning()) {
            try {
                executeCommand(String.format("box.cfg{memtx_memory=%d}", memtxMemory));
            } catch (Exception e) {
                logger().error(String.format("Failed to set memtx_memory to %d", memtxMemory), e);
                throw new RuntimeException(e);
            }
        }
        return this;
    }

    /**
     * Specify a directory in the classpath resource which will be mounted to the container.
     *
     * @param directoryResourcePath classpath resource directory full path
     * @return this container instance
     */
    public TarantoolContainer withDirectoryBinding(String directoryResourcePath) {
        checkNotRunning();
        this.directoryResourcePath = normalizePath(directoryResourcePath);
        return this;
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
    public TarantoolContainer withInstanceDir(String instanceDir) {
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
        return port;
    }

    /**
     * Specify the server init script file name
     *
     * @param scriptFileName script file path, relative to the mounted resource directory
     * @return this container instance
     * @see #withDirectoryBinding(String)
     */
    public TarantoolContainer withScriptFileName(String scriptFileName) {
        checkNotRunning();
        this.scriptFileName = scriptFileName;
        return this;
    }

    /**
     * Get the server init script file name
     *
     * @return file name
     */
    public String getScriptFileName() {
        return scriptFileName;
    }

    /**
     * Checks if already running and if so raises an exception to prevent too-late setters.
     */
    protected void checkNotRunning() {
        if (isRunning()) {
            throw new IllegalStateException("This option can be changed only before the container is running");
        }
    }

    private void checkServerScriptExists() {
        String serverScriptPath = Paths.get(getDirectoryBinding(), getScriptFileName()).toString();
        URL resource = getClass().getClassLoader().getResource(serverScriptPath);
        if (resource == null) {
            throw new RuntimeException(
                    String.format("Server configuration script %s is not found", serverScriptPath));
        }
    }

    @Override
    protected void configure() {
        checkServerScriptExists();

        URL sourceDirectory = getClass().getClassLoader().getResource(getDirectoryBinding());
        if (sourceDirectory == null) {
            throw new IllegalArgumentException(
                    String.format("No resource path found for the specified resource %s", getDirectoryBinding()));
        }
        String sourceDirectoryPath = normalizePath(sourceDirectory.getPath());

        //disable bind if directory is empty
        if (!sourceDirectoryPath.isEmpty()) {
            withFileSystemBind(sourceDirectoryPath, getInstanceDir(), BindMode.READ_WRITE);
        }

        if (useFixedPorts) {
            addFixedExposedPort(port, port);
        } else {
            addExposedPorts(port);
        }

        withCommand("tarantool", normalizePath(
                Paths.get(getInstanceDir(), getScriptFileName())));

        waitingFor(Wait.forLogMessage(".*entering the event loop.*", 1));
    }

    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo) {
        logger().info("Tarantool server is starting");
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo, boolean reused) {
        super.containerIsStarted(containerInfo, reused);

        withMemtxMemory(memtxMemory);
        withLogLevel(logLevel);

        logger().info("Tarantool server is listening at {}:{}", getHost(), getPort());
    }

    @Override
    protected void containerIsStopping(InspectContainerResponse containerInfo) {
        super.containerIsStopping(containerInfo);
        logger().info("Tarantool server is stopping");
    }

    @Override
    public Container.ExecResult executeScript(String scriptResourcePath) throws Exception {
        return clientHelper.executeScript(scriptResourcePath, this.sslContext);
    }

    @Override
    public <T> T executeScriptDecoded(String scriptResourcePath) throws Exception {
        return clientHelper.executeScriptDecoded(scriptResourcePath, this.sslContext);
    }

    @Override
    public Container.ExecResult executeCommand(String command) throws Exception {
        return clientHelper.executeCommand(command, this.sslContext);
    }

    @Override
    public <T> T executeCommandDecoded(String command) throws Exception {
        return clientHelper.executeCommandDecoded(command, this.sslContext);
    }

}
