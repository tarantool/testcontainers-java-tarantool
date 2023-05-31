package org.testcontainers.containers;

import org.testcontainers.utility.MountableFile;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

import static org.testcontainers.containers.PathUtils.normalizePath;

/**
 * Provides a wrapper around a Tarantool client with helper methods
 *
 * @author Alexey Kuzin
 * @author Artyom Dubinin
 * @author Ivan Dneprov
 */
public final class TarantoolContainerClientHelper {

    private static final String TMP_DIR = "/tmp";
    private static final Yaml yaml = new Yaml();

    private final TarantoolContainerOperations<? extends Container<?>> container;
    private final String tarantoolctlPath;
    private final String EXECUTE_SCRIPT_ERROR_TEMPLATE =
        "Executed script %s with exit code %d, stderr: \"%s\", stdout: \"%s\"";
    private final String EXECUTE_COMMAND_ERROR_TEMPLATE =
        "Executed command \"%s\" with exit code %d, stderr: \"%s\", stdout: \"%s\"";

    TarantoolContainerClientHelper(TarantoolContainerOperations<? extends Container<?>> container) {
        this.container = container;
        this.tarantoolctlPath = "tarantoolctl";
    }

    TarantoolContainerClientHelper(TarantoolContainerOperations<? extends Container<?>> container, String tarantoolctlPath) {
        this.container = container;
        this.tarantoolctlPath = tarantoolctlPath;
    }

    public Container.ExecResult executeScript(String scriptResourcePath, Boolean sslIsActive, String keyFile, String certFile) throws IOException, InterruptedException {
        if (!container.isRunning()) {
            throw new IllegalStateException("Cannot execute scripts in stopped container");
        }

        String scriptName = Paths.get(scriptResourcePath).getFileName().toString();
        String containerPath = normalizePath(Paths.get(TMP_DIR, scriptName));
        container.copyFileToContainer(MountableFile.forClasspathResource(scriptResourcePath), containerPath);
        return executeCommand(String.format("return dofile('%s')", containerPath), sslIsActive, keyFile, certFile);
    }

    public <T> T executeScriptDecoded(String scriptResourcePath, Boolean sslIsActive, String keyFile, String certFile) throws IOException, InterruptedException, ExecutionException {
        Container.ExecResult result = executeScript(scriptResourcePath, sslIsActive, keyFile, certFile);

        if (result.getExitCode() != 0) {

            if (result.getExitCode() == 3) {
                throw new ExecutionException(String.format(EXECUTE_SCRIPT_ERROR_TEMPLATE,
                    scriptResourcePath, result.getExitCode(),
                    result.getStderr(), result.getStdout()),
                    new Throwable());
            }

            throw new IllegalStateException(String.format(EXECUTE_SCRIPT_ERROR_TEMPLATE,
                scriptResourcePath, result.getExitCode(),
                result.getStderr(), result.getStdout()));
        }

        return yaml.load(result.getStdout());
    }

    public Container.ExecResult executeCommand(String command, Boolean sslIsActive, String keyFile, String certFile) throws IOException, InterruptedException {
        if (!container.isRunning()) {
            throw new IllegalStateException("Cannot execute commands in stopped container");
        }

        command = command.replace("\"", "\\\"");

        String bashCommand;
        if (keyFile != "" && certFile != "") {
            bashCommand = String.format("echo \"print(require('yaml').encode(require('net.box').connect({ uri='%s:%d', " +
                    "params = { transport='ssl', ssl_key_file = '%s', ssl_cert_file = '%s' }}, " +
                    "{ user = '%s', password = '%s' } ):eval('return %s'))); os.exit();\" | tarantool",
                container.getHost(), container.getInternalPort(),
                keyFile, certFile,
                container.getUsername(), container.getPassword(),
                command
            );
        } else if (sslIsActive) {
                bashCommand = String.format("echo \"print(require('yaml').encode(require('net.box').connect({ uri='%s:%d', " +
                        "params = { transport='ssl' }}, " +
                        "{ user = '%s', password = '%s' } ):eval('return %s'))); os.exit();\" | tarantool",
                container.getHost(), container.getInternalPort(),
                container.getUsername(), container.getPassword(),
                command
                );
        } else {
            bashCommand = String.format("echo \"%s\" | %s connect %s:%s@%s:%s",
                command, tarantoolctlPath, container.getUsername(), container.getPassword(),
                container.getHost(), container.getInternalPort());
        }

        return container.execInContainer("sh", "-c", bashCommand);
    }

    public <T> T executeCommandDecoded(String command, Boolean sslIsActive, String keyFile, String certFile) throws IOException, InterruptedException {
        Container.ExecResult result = executeCommand(command, sslIsActive, keyFile, certFile);

        if (result.getExitCode() != 0) {
            throw new IllegalStateException(String.format(EXECUTE_COMMAND_ERROR_TEMPLATE,
                command, result.getExitCode(), result.getStderr(), result.getStdout()));
        }

        return yaml.load(result.getStdout());
    }

}
