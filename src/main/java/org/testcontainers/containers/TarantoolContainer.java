package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.containers.wait.strategy.Wait;

import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.testcontainers.containers.PathUtils.normalizeBindingPath;

/**
 * Sets up a Tarantool instance and provides API for configuring it.
 *
 * @author Alexey Kuzin
 */
public class TarantoolContainer extends GenericContainer<TarantoolContainer>
        implements TarantoolContainerOperations<TarantoolContainer> {

    public static final String TARANTOOL_IMAGE = "tarantool/tarantool";
    public static final String DEFAULT_IMAGE_VERSION = "2.x-centos7";
    public static final String DEFAULT_TARANTOOL_BASE_IMAGE =
            String.format("%s:%s", TARANTOOL_IMAGE, DEFAULT_IMAGE_VERSION);

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

    private final TarantoolContainerClientHelper clientHelper;

    public TarantoolContainer() {
        this(String.format("%s:%s", TARANTOOL_IMAGE, DEFAULT_IMAGE_VERSION));
    }

    public TarantoolContainer(String dockerImageName) {
        super(dockerImageName);
        clientHelper = new TarantoolContainerClientHelper(this);
    }

    public TarantoolContainer(Future<String> image) {
        super(image);
        clientHelper = new TarantoolContainerClientHelper(this);
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
     * Change the log_level setting on the Tarantool instance
     *
     * @param logLevel new log_level value
     * @return this container instance
     */
    public TarantoolContainer withLogLevel(TarantoolLogLevel logLevel) {
        this.logLevel = logLevel;
        if (isRunning()) {
            try {
                executeCommand(logLevel.toCommand()).get();
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
                executeCommand(String.format("box.cfg{memtx_memory=%d}", memtxMemory)).get();
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
        this.directoryResourcePath = normalizeBindingPath(directoryResourcePath);
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

    @Override
    protected void configure() {
        URL sourceDirectory = getClass().getClassLoader().getResource(getDirectoryBinding());
        if (sourceDirectory == null) {
            throw new IllegalArgumentException(
                    String.format("No resource path found for the specified resource %s", getDirectoryBinding()));
        }
        String sourceDirectoryPath = normalizeBindingPath(sourceDirectory.getPath());

        withFileSystemBind(sourceDirectoryPath, getInstanceDir(), BindMode.READ_WRITE);
        withExposedPorts(port);

        withCommand("tarantool",
                Paths.get(getInstanceDir(), getScriptFileName()).toString().replace('\\','/'));

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
    public CompletableFuture<List<?>> executeScript(String scriptResourcePath) throws Exception {
        return clientHelper.executeScript(scriptResourcePath);
    }

    @Override
    public CompletableFuture<List<?>> executeCommand(String command, Object... arguments) throws Exception {
        return clientHelper.executeCommand(command, arguments);
    }
}
