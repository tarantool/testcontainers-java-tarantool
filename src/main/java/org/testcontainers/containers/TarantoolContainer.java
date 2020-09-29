package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import io.tarantool.driver.StandaloneTarantoolClient;
import io.tarantool.driver.TarantoolClient;
import io.tarantool.driver.TarantoolClientConfig;
import io.tarantool.driver.TarantoolServerAddress;
import io.tarantool.driver.auth.SimpleTarantoolCredentials;
import io.tarantool.driver.auth.TarantoolCredentials;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Sets up a Tarantool instance and provides API for configuring it
 *
 * @author Alexey Kuzin
 */
public class TarantoolContainer extends GenericContainer<TarantoolContainer> {

    public static final String TARANTOOL_IMAGE = "tarantool/tarantool";
    public static final String DEFAULT_IMAGE_VERSION = "2.x-centos7";

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 3301;
    private static final String API_USER = "api_user";
    private static final String API_PASSWORD = "secret";
    private static final String SCRIPT_RESOURCE_PATH = "org/testcontainers/containers/server.lua";

    private static final String INSTANCE_DIR = "/app";
    private static final String INSTANCE_PATH = INSTANCE_DIR + "/server.lua";

    private String username = API_USER;
    private String password = API_PASSWORD;
    private String host = DEFAULT_HOST;
    private Integer port = DEFAULT_PORT;
    private String scriptResourcePath = SCRIPT_RESOURCE_PATH;

    public TarantoolContainer() {
        this(String.format("%s:%s", TARANTOOL_IMAGE, DEFAULT_IMAGE_VERSION));
    }

    public TarantoolContainer(String dockerImageName) {
        super(dockerImageName);
    }

    @Override
    public String getHost() {
        return host;
    }

    public int getPort() {
        return getMappedPort(port);
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    /**
     * Specify the server init script
     * @param scriptResourcePath classpath resource full path
     */
    public void withScriptBinding(String scriptResourcePath) {
        checkNotRunning();
        this.scriptResourcePath = scriptResourcePath;
    }

    /**
     * Checks if already running and if so raises an exception to prevent too-late setters.
     */
    private void checkNotRunning() {
        if (isRunning()) {
            throw new IllegalStateException("This option can be changed only before the container is running");
        }
    }

    @Override
    protected void configure() {

        withClasspathResourceMapping(scriptResourcePath, INSTANCE_PATH, BindMode.READ_ONLY);
        withExposedPorts(port);
        withCommand("tarantool", INSTANCE_PATH);

        waitingFor(new LogMessageWaitStrategy()
                .withRegEx(".*entering the event loop.*")
                .withTimes(1));
    }

    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo) {
        logger().info("Tarantool server is starting");
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo, boolean reused) {
        super.containerIsStarted(containerInfo, reused);
        logger().info("Tarantool server is listening at {}:{}", getHost(), getPort());
    }

    /**
     * Execute a local script in the Tarantool instance. The path must be classpath-relative.
     * `dofile()` function is executed internally, so possible exceptions will be caught as the client exceptions.
     *
     * @param scriptResourcePath the classpath resource path to a script
     * @return script execution result
     * @throws Exception if failed to connect to the instance or execution fails
     */
    public CompletableFuture<List<Object>> executeScript(String scriptResourcePath) throws Exception {
        if (!isRunning()) {
            throw new IllegalStateException("Cannot execute scripts in stopped container");
        }
        String scriptName = Paths.get(scriptResourcePath).getFileName().toString();
        String containerPath = Paths.get(INSTANCE_DIR, scriptName).toString();
        this.copyFileToContainer(
                MountableFile.forClasspathResource(scriptResourcePath), containerPath);
        return executeCommand(String.format("dofile('%s')", containerPath));
    }

    /**
     * Execute a command in the Tarantool instance. Example of a command: `return 1 + 2, 'foo'`
     *
     * @param command a valid Lua command or a sequence of Lua commands
     * @param arguments command arguments
     * @return command execution result
     * @throws Exception if failed to connect to the instance or execution fails
     */
    public CompletableFuture<List<Object>> executeCommand(String command, Object... arguments) throws Exception {
        if (!isRunning()) {
            throw new IllegalStateException("Cannot execute commands in stopped container");
        }

        TarantoolCredentials credentials = new SimpleTarantoolCredentials(getUsername(), getPassword());
        TarantoolClientConfig config = TarantoolClientConfig.builder()
                .withCredentials(credentials).build();
        TarantoolServerAddress address = new TarantoolServerAddress(getHost(), getPort());
        try (TarantoolClient client = new StandaloneTarantoolClient(config, address)) {
            return client.eval(command, Arrays.asList(arguments));
        } catch (Exception e) {
            logger().error("Failed to execute command '{}': {}", command, e.getMessage());
            throw e;
        }
    }
}
