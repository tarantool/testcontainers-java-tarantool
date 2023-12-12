package org.testcontainers.containers;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Alexey Kuzin
 * @author Ivan Dneprov
 */
class TarantoolContainerTest {

    private static final String ENV_TARANTOOL_VERSION = "TARANTOOL_VERSION";

    private void addEnv (String key, String value) throws NoSuchFieldException, IllegalAccessException {
        Class<?> classOfMap = System.getenv().getClass();
        Field field = classOfMap.getDeclaredField("m");
        field.setAccessible(true);
        Map<String, String> writeableEnvironmentVariables = (Map<String, String>) field.get(System.getenv());
        writeableEnvironmentVariables.put(key, value);
        field.setAccessible(false);
    }

    private void removeEnv (String key, String value) throws NoSuchFieldException, IllegalAccessException {
        Class<?> classOfMap = System.getenv().getClass();
        Field field = classOfMap.getDeclaredField("m");
        field.setAccessible(true);
        Map<String, String> writeableEnvironmentVariables = (Map<String, String>) field.get(System.getenv());
        writeableEnvironmentVariables.remove(key);
        field.setAccessible(false);
    }
    @Test
    public void testExecuteScript() throws Exception {
        try (TarantoolContainer container = new TarantoolContainer()) {
            container.start();
            container.executeScript("org/testcontainers/containers/test.lua");
            List<?> result = container.executeCommandDecoded("return user_function_no_param()");
            Assertions.assertEquals(1, result.size());
            Assertions.assertEquals(5, result.get(0));
        }
    }

    @Test
    public void testContainerWithParameters() throws Exception {
        int memory = 256 * 1024 * 1024;
        try (TarantoolContainer container = new TarantoolContainer()
                .withDirectoryBinding("io/tarantool")
                .withScriptFileName("custom.lua")
                .withUsername("uuuser")
                .withPassword("secret")
                .withMemtxMemory(memory)
                .withLogLevel(TarantoolLogLevel.INFO)) {
            container.start();

            List<?> result = container.executeCommandDecoded("return box.cfg.memtx_memory");
            Assertions.assertEquals(1, result.size());
            Assertions.assertEquals(memory, result.get(0));

            result = container.executeCommandDecoded("return box.cfg.log_level");
            Assertions.assertEquals(1, result.size());
            Assertions.assertEquals(5, result.get(0));

            result = container.executeCommandDecoded("return user_function_no_param()");
            Assertions.assertEquals(result.size(), 1);
            Assertions.assertEquals(result.get(0), 5);
        }
    }

    @Test
    public void testContainerWithTrueVersion() throws Exception {
        final String version = "2.11.0";
        addEnv(ENV_TARANTOOL_VERSION, version);
        try (TarantoolContainer container = new TarantoolContainer()) {
            container.start();
        }
        removeEnv(ENV_TARANTOOL_VERSION, version);
    }

    @Test
    public void testContainerWithDefaultVersionVersion() throws Exception {
        try (TarantoolContainer container = new TarantoolContainer()) {
            container.start();
        }
    }

    @Test
    public void testContainerWithEmptyVersion() throws Exception {
        final String version = "  ";
        addEnv(ENV_TARANTOOL_VERSION, version);
        try (TarantoolContainer container = new TarantoolContainer()) {
            container.start();
        }
        removeEnv(ENV_TARANTOOL_VERSION, version);
    }

    @Test
    public void testContainerWithWrongVersion() throws Exception {
        final String version = "wrong_version";
        addEnv(ENV_TARANTOOL_VERSION, version);
        try (TarantoolContainer container = new TarantoolContainer()) {
            container.start();
        }catch (Exception exc) {
            Assertions.assertEquals(ContainerFetchException.class, exc.getClass());
        }
        removeEnv(ENV_TARANTOOL_VERSION, version);
    }
}
