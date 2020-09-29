package org.testcontainers.containers;

import org.junit.ClassRule;
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
    private static TarantoolContainer container = new TarantoolContainer();

    @Test
    public void testSetLogLevel() throws Exception {
        container.withLogLevel(TarantoolLogLevel.INFO);
        List<Object> result = container.executeCommand("return box.cfg.log_level").get();
        assertEquals(1, result.size());
        assertEquals(5, result.get(0));
    }
    @Test
    public void testSetMemtxMemory() throws Exception {
        int memory = 256 * 1024 * 1024;
        container.withMemtxMemory(memory);
        List<Object> result = container.executeCommand("return box.cfg.memtx_memory").get();
        assertEquals(1, result.size());
        assertEquals(memory, result.get(0));
    }
}
