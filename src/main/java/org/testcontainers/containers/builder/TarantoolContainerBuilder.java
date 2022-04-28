package org.testcontainers.containers.builder;

import org.testcontainers.containers.DefaultTarantoolContainer;
import org.testcontainers.containers.TarantoolLogLevel;

public interface TarantoolContainerBuilder {

    /**
     * Use fixed ports binding.
     * Defaults to false.
     *
     * @return HTTP API port
     */
    TarantoolContainerBuilder withUseFixedPorts(boolean useFixedPorts);

    /**
     * Specify the host for connecting to Tarantool with.
     *
     * @param host valid IP address or hostname
     * @return this container instance
     */
    TarantoolContainerBuilder withHost(String host);

    /**
     * Specify the port for connecting to Tarantool with.
     *
     * @param port valid port number
     * @return this container instance
     */
    TarantoolContainerBuilder withPort(int port);

    /**
     * Specify the username for connecting to Tarantool with.
     * Warning! This user must be created on Tarantool instance startup, e.g. specified in the startup script.
     *
     * @param username the client user name
     * @return this container instance
     */
    TarantoolContainerBuilder withUsername(String username);

    /**
     * Specify the password for the specified user for connecting to Tarantool with.
     * Warning! This user must be created on Tarantool instance startup, e.g. specified in the startup script,
     * together with setting the password.
     *
     * @param password the client user password
     * @return this container instance
     */
    TarantoolContainerBuilder withPassword(String password);

    /**
     * Change the log_level setting on the Tarantool instance
     *
     * @param logLevel new log_level value
     * @return this container instance
     */
    TarantoolContainerBuilder withLogLevel(TarantoolLogLevel logLevel);

    /**
     * Change the memtx_memory setting on the Tarantool instance
     *
     * @param memtxMemory new memtx_memory value, must be greater than 0
     * @return this container instance
     */
    TarantoolContainerBuilder withMemtxMemory(Integer memtxMemory);

    /**
     * Specify a directory in the classpath resource which will be mounted to the container.
     *
     * @param directoryResourcePath classpath resource directory full path
     * @return this container instance
     */
    TarantoolContainerBuilder withDirectoryBinding(String directoryResourcePath);

    /**
     * Specify the directory inside container that the resource directory will be mounted to. The default value is
     * "/app".
     *
     * @param instanceDir valid directory path
     * @return this container instance
     */
    TarantoolContainerBuilder withInstanceDir(String instanceDir);

    /**
     * Specify the server init script file name
     *
     * @param scriptFileName script file path, relative to the mounted resource directory
     * @return this container instance
     * @see #withDirectoryBinding(String)
     */
    TarantoolContainerBuilder withScriptFileName(String scriptFileName);

    DefaultTarantoolContainer build();
}
