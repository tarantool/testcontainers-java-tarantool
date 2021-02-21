package org.testcontainers.containers;

import io.tarantool.driver.ClusterTarantoolTupleClient;
import io.tarantool.driver.TarantoolClientConfig;
import io.tarantool.driver.TarantoolServerAddress;
import io.tarantool.driver.api.TarantoolClient;
import io.tarantool.driver.api.TarantoolResult;
import io.tarantool.driver.api.tuple.TarantoolTuple;
import io.tarantool.driver.auth.SimpleTarantoolCredentials;
import io.tarantool.driver.auth.TarantoolCredentials;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Provides a wrapper around a Tarantool client with helper methods
 *
 * @author Alexey Kuzin
 */
public final class TarantoolContainerClientHelper {

    private static final String TMP_DIR = "/tmp";

    private final TarantoolContainerOperations<? extends Container<?>> container;
    private final AtomicReference<TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>>> clientHolder =
            new AtomicReference<>();

    TarantoolContainerClientHelper(TarantoolContainerOperations<? extends Container<?>> container) {
        this.container = container;
    }

    private TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>>
    createClient(TarantoolClientConfig config, TarantoolServerAddress address) {
        return new ClusterTarantoolTupleClient(config, address);
    }

    /**
     * Configure or return an already configured client connected to a Cartridge router
     *
     * @param config router instance client config
     * @param address router host address
     * @return a configured client
     */
    public TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>>
    getClient(TarantoolClientConfig config, TarantoolServerAddress address) {
        if (!container.isRunning()) {
            throw new IllegalStateException("Cannot connect to Tarantool instance in a stopped container");
        }
        if (clientHolder.get() == null) {
            clientHolder.compareAndSet(null, createClient(config, address));
        }
        return clientHolder.get();
    }

    public CompletableFuture<List<?>> executeScript(String scriptResourcePath) {
        if (!container.isRunning()) {
            throw new IllegalStateException("Cannot execute scripts in stopped container");
        }

        String scriptName = Paths.get(scriptResourcePath).getFileName().toString();
        String containerPath = Paths.get(TMP_DIR, scriptName).toString().replace('\\','/');
        container.copyFileToContainer(MountableFile.forClasspathResource(scriptResourcePath), containerPath);
        return executeCommand(String.format("dofile('%s')", containerPath));
    }

    public CompletableFuture<List<?>> executeCommand(String command, Object... arguments) {
        if (!container.isRunning()) {
            throw new IllegalStateException("Cannot execute commands in stopped container");
        }

        TarantoolCredentials credentials = new SimpleTarantoolCredentials(
                container.getUsername(), container.getPassword());
        TarantoolServerAddress address = new TarantoolServerAddress(container.getHost(), container.getPort());
        TarantoolClientConfig config = TarantoolClientConfig.builder().withCredentials(credentials).build();
        return getClient(config, address).eval(command, Arrays.asList(arguments));
    }
}
