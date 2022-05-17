package org.testcontainers.containers;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Alexey Kuzin
 */
@Testcontainers
public class TarantoolCartridgeStaticContainerTest {

    @Container
    private static final TarantoolCartridgeContainer container =
            new TarantoolCartridgeContainer(
                    "Dockerfile",
                    "tarantool-community:latest",
                    "cartridge/instances.yml",
                    "cartridge/topology.lua")
                    .withDirectoryBinding("cartridge")
                    .withStartupTimeout(Duration.ofSeconds(300))
                    .withLogConsumer(new Slf4jLogConsumer(
                            LoggerFactory.getLogger(TarantoolCartridgeStaticContainerTest.class)));

    @Test
    public void test_StaticClusterContainer_StartsSuccessfully_ifDirectoryBindingIsUsed() throws Exception {
        CartridgeContainerTestUtils.executeProfileReplaceSmokeTest(container);
    }


    @Test
    public void test_migrations_migrator() throws Exception {
        container.executeCommand("require('migrator').up()").get();
    }

    @Test
    public void test_migrations_curl() throws IOException, InterruptedException {

        String urlStr = "http://" + container.getRouterHost() + ":" + "8081" + "/migrations/up";
        int code = -1;
        org.testcontainers.containers.Container.ExecResult answer = container.execInContainer("curl", "-X", "POST", urlStr);
        code = answer.getExitCode();
        Assert.assertEquals(0, code);
    }

    @Test
    public void test_migrations_http() throws Exception {

        HttpURLConnection connection;
        OutputStream os = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bfR = null;
        StringBuilder strBuilder = new StringBuilder();

        Map<String, String> bodyHttpPostRequest = new HashMap<>();
        byte[] outSteamByte = bodyHttpPostRequest.toString().getBytes(StandardCharsets.UTF_8);

        try {
            String urlStr = "http://" + container.getRouterHost() + ":" + container.getAPIPort() + "/migrations/up";
            connection = createConnection(urlStr);
            os = connection.getOutputStream();
            os.write(outSteamByte);

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                inputStreamReader = new InputStreamReader(connection.getInputStream());
                bfR = new BufferedReader(inputStreamReader);
                String line;
                while ((line = bfR.readLine()) != null) {
                    strBuilder.append(line);
                }
            }

        } catch (MalformedURLException ex) {
        } catch (IOException e) {
        } finally {
            inputStreamReader.close();
            os.close();
            bfR.close();
        }
        Assert.assertTrue(strBuilder.toString().contains("applied"));
    }

    private HttpURLConnection createConnection(String urlStr) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlStr).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout(200);
        connection.setReadTimeout(200);
        connection.connect();

        return connection;
    }
}
