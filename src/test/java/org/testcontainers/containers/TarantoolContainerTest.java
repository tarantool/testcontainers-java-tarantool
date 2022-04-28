package org.testcontainers.containers;

import org.junit.Test;
import org.testcontainers.containers.builder.TarantoolContainerBuilderImpl;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Alexey Kuzin
 */
public class TarantoolContainerTest {

    @Test
    public void testExecuteScript() throws Exception {
        try (TarantoolContainer<DefaultTarantoolContainerImpl> container = new DefaultTarantoolContainerImpl()) {
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
        try (TarantoolContainer<DefaultTarantoolContainerImpl> container = new TarantoolContainerBuilderImpl()
                .withDirectoryBinding("io/tarantool")
                .withScriptFileName("custom.lua")
                .withUsername("uuuser")
                .withPassword("secret")
                .withMemtxMemory(memory)
                .withLogLevel(TarantoolLogLevel.INFO)
                .build()
        ) {
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