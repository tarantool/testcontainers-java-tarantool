package org.testcontainers.containers;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.junit.Assert.assertEquals;

/**
 * @author Alexey Kuzin
 * @author Oleg Kuznetsov
 * @author Ivan Dneprov
 */
@Testcontainers
public class TarantoolStaticContainerIT {

    @Container
    private static final TarantoolContainer container = new TarantoolContainer();

    @Test
    public void testExecuteCommand() throws Exception {
        List<?> result = container.executeCommandDecoded("return 1, 2");
        assertEquals(2, result.size());
        assertEquals(1, result.get(0));
    }

    @Test
    public void testExecuteCommandWithArguments() throws Exception {
        List<?> result = container.executeCommandDecoded(
                "return require('fun').iter({1, 2, 3}):reduce(function(x, acc) return acc+x end, 0)");
        assertEquals(1, result.size());
        assertEquals(6, result.get(0));
    }

    @Test
    public void testSetLogLevel() throws Exception {
        container.withLogLevel(TarantoolLogLevel.INFO);
        List<?> result = container.executeCommandDecoded("return box.cfg.log_level");
        assertEquals(1, result.size());
        assertEquals(5, result.get(0));
    }

    @Test
    public void testSetMemtxMemory() throws Exception {
        int memory = 256 * 1024 * 1024;
        container.withMemtxMemory(memory);
        List<?> result = container.executeCommandDecoded("return box.cfg.memtx_memory");
        assertEquals(1, result.size());
        assertEquals(memory, result.get(0));
    }
}
