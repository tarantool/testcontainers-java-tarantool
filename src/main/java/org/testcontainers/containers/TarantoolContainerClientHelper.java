package org.testcontainers.containers;

import io.tarantool.driver.api.TarantoolClient;
import io.tarantool.driver.api.TarantoolClientFactory;
import io.tarantool.driver.api.TarantoolResult;
import io.tarantool.driver.api.retry.TarantoolRequestRetryPolicies;
import io.tarantool.driver.api.tuple.TarantoolTuple;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.testcontainers.containers.PathUtils.normalizePath;

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

    private TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>> createClient() {
        return TarantoolClientFactory.createClient()
                .withCredentials(container.getUsername(), container.getPassword())
                .withAddress(container.getHost(), container.getPort())
                .withRequestTimeout(5000)
                .withRetryingByNumberOfAttempts(15,
                        TarantoolRequestRetryPolicies.retryNetworkErrors()
                                .or(TarantoolRequestRetryPolicies.retryTarantoolNoSuchProcedureErrors()),
                        b -> b.withDelay(100))
                .build();
    }

    /**
     * Configure or return an already configured client connected to a Cartridge router
     *
     * @return a configured client
     */
    public TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>> getClient() {
        if (!container.isRunning()) {
            throw new IllegalStateException("Cannot connect to Tarantool instance in a stopped container");
        }
        if (clientHolder.get() == null) {
            clientHolder.compareAndSet(null, createClient());
        }
        return clientHolder.get();
    }

    public CompletableFuture<List<?>> executeScript(String scriptResourcePath) {
        if (!container.isRunning()) {
            throw new IllegalStateException("Cannot execute scripts in stopped container");
        }

        String scriptName = Paths.get(scriptResourcePath).getFileName().toString();
        String containerPath = normalizePath(Paths.get(TMP_DIR, scriptName));
        container.copyFileToContainer(MountableFile.forClasspathResource(scriptResourcePath), containerPath);
        return executeCommand(String.format("dofile('%s')", containerPath));
    }

    public CompletableFuture<List<?>> executeCommand(String command, Object... arguments) {
        if (!container.isRunning()) {
            throw new IllegalStateException("Cannot execute commands in stopped container");
        }

        return getClient().eval(command, Arrays.asList(arguments));
    }
}
