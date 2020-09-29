package org.testcontainers.containers;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Alexey Kuzin
 */
class TarantoolContainerTest {

    @Test
    public void testExecuteCommand() throws Exception {
        try (TarantoolContainer container = new TarantoolContainer()) {
            container.start();

            List<Object> result = container.executeCommand("return 1, 2").get();
            assertEquals(result.size(), 2);
            assertEquals(result.get(0), 1);
        } catch (Exception e) {
            throw e;
        }
    }

    @Test
    public void testExecuteScript() throws Exception {
        try (TarantoolContainer container = new TarantoolContainer()) {
            container.start();

            container.executeScript("org/testcontainers/containers/test.lua").get();
            List<Object> result = container.executeCommand("return user_function_no_param()").get();
            assertEquals(result.size(), 1);
            assertEquals(result.get(0), 5);
        } catch (Exception e) {
            throw e;
        }
    }
}