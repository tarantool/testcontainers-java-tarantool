package org.testcontainers.containers;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Alexey Kuzin
 */
@Testcontainers
public class TarantoolStaticContainerTest {

    @Container
    private static final TarantoolContainer container = new TarantoolContainer();

    @Test
    public void testExecuteCommand() throws Exception {
        List<?> result = container.executeCommand("return 1, 2").get();
        assertEquals(2, result.size());
        assertEquals(1, result.get(0));
    }

    @Test
    public void testExecuteCommandWithArguments() throws Exception {
        List<?> result = container.executeCommand(
                "return require('fun').iter({...}):reduce(function(x, acc) return acc+x end, 0)",
                1, 2, 3)
                .get();
        assertEquals(1, result.size());
        assertEquals(6, result.get(0));
    }

    @Test
    public void testSetLogLevel() throws Exception {
        container.withLogLevel(TarantoolLogLevel.INFO);
        List<?> result = container.executeCommand("return box.cfg.log_level").get();
        assertEquals(1, result.size());
        assertEquals(5, result.get(0));
    }

    @Test
    public void testSetMemtxMemory() throws Exception {
        int memory = 256 * 1024 * 1024;
        container.withMemtxMemory(memory);
        List<?> result = container.executeCommand("return box.cfg.memtx_memory").get();
        assertEquals(1, result.size());
        assertEquals(memory, result.get(0));
    }
}
