package org.testcontainers.containers;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Alexey Kuzin
 */
class TarantoolContainerTest {

    @Test
    public void testExecuteScript() throws Exception {
        try (TarantoolContainer<?> container = new TarantoolContainer<>()) {
            container.start();

            container.executeScript("org/testcontainers/containers/test.lua").get();
            List<?> result = container.executeCommand("return user_function_no_param()").get();
            assertEquals(1, result.size());
            assertEquals(5, result.get(0));
        }
    }

    @Test
    public void testContainerWithParameters() throws Exception {
        int memory = 256 * 1024 * 1024;
        try (TarantoolContainer<?> container = new TarantoolContainer<>()
                .withDirectoryBinding("io/tarantool")
                .withScriptFileName("custom.lua")
                .withUsername("uuuser")
                .withPassword("secret")
                .withMemtxMemory(memory)
                .withLogLevel(TarantoolLogLevel.INFO)) {
            container.start();

            List<?> result = container.executeCommand("return box.cfg.memtx_memory").get();
            assertEquals(1, result.size());
            assertEquals(memory, result.get(0));

            result = container.executeCommand("return box.cfg.log_level").get();
            assertEquals(1, result.size());
            assertEquals(5, result.get(0));

            result = container.executeCommand("return user_function_no_param()").get();
            assertEquals(result.size(), 1);
            assertEquals(result.get(0), 5);
        }
    }
}