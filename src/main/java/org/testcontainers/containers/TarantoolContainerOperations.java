package org.testcontainers.containers;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Represents operations available on a Tarantool Container
 *
 * @author Alexey Kuzin
 */
public interface TarantoolContainerOperations<T extends Container<T>> extends Container<T> {

    //todo: move to TarantoolContainer interface after refactoring TarantoolCartridgeContainer
    TarantoolContainerSettings getSettings();

    /**
     * Execute a local script in the Tarantool instance. The path must be classpath-relative.
     * `dofile()` function is executed internally, so possible exceptions will be caught as the client exceptions.
     *
     * @param scriptResourcePath the classpath resource path to a script
     * @return script execution result
     * @throws Exception if failed to connect to the instance or execution fails
     */
    CompletableFuture<List<?>> executeScript(String scriptResourcePath) throws Exception;

    /**
     * Execute a command in the Tarantool instance. Example of a command: `return 1 + 2, 'foo'`
     *
     * @param command   a valid Lua command or a sequence of Lua commands
     * @param arguments command arguments
     * @return command execution result
     * @throws Exception if failed to connect to the instance or execution fails
     */
    CompletableFuture<List<?>> executeCommand(String command, Object... arguments) throws Exception;
}
