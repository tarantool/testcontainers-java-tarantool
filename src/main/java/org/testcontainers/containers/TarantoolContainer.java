package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;
import org.testcontainers.lifecycle.Startable;

public interface TarantoolContainer<SELF extends GenericContainer<SELF>>
        extends TarantoolContainerOperations<SELF>, Container<SELF>, AutoCloseable, WaitStrategyTarget, Startable {

    TarantoolContainerSettings getSettings();

    /**
     * Change the log_level setting on the Tarantool instance
     *
     * @param logLevel new log_level value
     * @return this container instance
     */
    TarantoolContainer<SELF> withLogLevel(TarantoolLogLevel logLevel);

    /**
     * Change the memtx_memory setting on the Tarantool instance
     *
     * @param memtxMemory new memtx_memory value, must be greater than 0
     * @return this container instance
     */
    TarantoolContainer<SELF> withMemtxMemory(Integer memtxMemory);
}
