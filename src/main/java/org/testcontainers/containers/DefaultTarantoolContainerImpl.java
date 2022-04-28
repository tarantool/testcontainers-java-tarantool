package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.containers.wait.strategy.Wait;

import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.testcontainers.containers.PathUtils.normalizePath;

/**
 * Sets up a Tarantool instance and provides API for configuring it.
 *
 * @author Alexey Kuzin
 */
public class DefaultTarantoolContainerImpl
        extends GenericContainer<DefaultTarantoolContainerImpl>
        implements DefaultTarantoolContainer {

    private final TarantoolContainerSettings settings;
    private final TarantoolContainerClientHelper clientHelper;

    public DefaultTarantoolContainerImpl() {
        this(new TarantoolContainerSettings());
    }

    public DefaultTarantoolContainerImpl(TarantoolContainerSettings settings) {
        super(settings.getImageName());
        this.settings = settings;
        this.clientHelper = new TarantoolContainerClientHelper(this);
    }

    @Override
    public TarantoolContainerSettings getSettings() {
        return this.settings;
    }

    @Override
    public CompletableFuture<List<?>> executeScript(String scriptResourcePath) {
        return clientHelper.executeScript(scriptResourcePath);
    }

    @Override
    public CompletableFuture<List<?>> executeCommand(String command, Object... arguments) {
        return clientHelper.executeCommand(command, arguments);
    }

    @Override
    public DefaultTarantoolContainerImpl withLogLevel(TarantoolLogLevel logLevel) {
        this.settings.setLogLevel(logLevel);
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

    @Override
    public DefaultTarantoolContainerImpl withMemtxMemory(Integer memtxMemory) {
        if (memtxMemory <= 0) {
            throw new RuntimeException(
                    String.format("The specified memtx_memory value must be >= 0, but was %d", memtxMemory));
        }
        this.settings.setMemtxMemory(memtxMemory);
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

    @Override
    protected void configure() {
        checkServerScriptExists();

        final String directoryResourcePath = settings.getDirectoryResourcePath();
        URL sourceDirectory = getClass().getClassLoader().getResource(directoryResourcePath);
        if (sourceDirectory == null) {
            throw new IllegalArgumentException(
                    String.format("No resource path found for the specified resource %s", directoryResourcePath));
        }
        String sourceDirectoryPath = normalizePath(sourceDirectory.getPath());

        //disable bind if directory is empty
        if (!sourceDirectoryPath.isEmpty()) {
            withFileSystemBind(sourceDirectoryPath, settings.getInstanceDir(), BindMode.READ_WRITE);
        }

        if (settings.isUseFixedPorts()) {
            addFixedExposedPort(settings.getPort(), settings.getPort());
        } else {
            withExposedPorts(settings.getPort());
        }

        withCommand("tarantool", normalizePath(
                Paths.get(settings.getInstanceDir(), settings.getScriptFileName())));

        waitingFor(Wait.forLogMessage(".*entering the event loop.*", 1));
    }

    private void checkServerScriptExists() {
        String serverScriptPath = Paths.get(
                settings.getDirectoryResourcePath(), settings.getScriptFileName()).toString();
        URL resource = getClass().getClassLoader().getResource(serverScriptPath);
        if (resource == null) {
            throw new RuntimeException(
                    String.format("Server configuration script %s is not found", serverScriptPath));
        }
    }

    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo) {
        logger().info("Tarantool server is starting");
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo, boolean reused) {
        super.containerIsStarted(containerInfo, reused);

        //Why don't work for bundle: "Incorrect value for option 'memtx_memory': cannot decrease memory size at runtime"
//        withMemtxMemory(settings.getMemtxMemory());
//        withLogLevel(settings.getLogLevel());

        logger().info("Tarantool server is listening at {}:{}", getHost(), settings.getPort());
    }

    @Override
    protected void containerIsStopping(InspectContainerResponse containerInfo) {
        super.containerIsStopping(containerInfo);
        logger().info("Tarantool server is stopping");
    }
}
