package org.testcontainers.containers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TarantoolCartridgePortMappingTest {

    @Container
    private final static TarantoolCartridgeContainer container = new TarantoolCartridgeContainer(
            "Dockerfile",
            "mapping-ports-container",
            "cartridge/instances.yml",
            "cartridge/replicasets.yml")
            .withEnv(TarantoolCartridgeContainer.ENV_TARANTOOL_CLUSTER_COOKIE, "secret")
            .withRouterUsername("admin")
            .withRouterPassword("secret")
            .withStartupTimeout(Duration.ofMinutes(5))
            .withLogConsumer(new Slf4jLogConsumer(
                    LoggerFactory.getLogger(TarantoolCartridgeBootstrapFromYamlTest.class)));

    @Test
    void portMappingTest() throws IOException {

        final int httpPortToFirstRouter = 8081;
        final int httpPortToSecondRouter = 8082;
        final int portToFirstRouter = 3301;
        final int portToSecondRouter = 3302;
        final String url = "localhost";

        container.addExposedPorts(httpPortToFirstRouter, httpPortToSecondRouter);
        container.start();

        final StringBuilder curlCommandToConnectToRouters = new StringBuilder("http://localhost:")
                .append(container.getMappedPort(httpPortToFirstRouter));

        // send get request to first router via http
        HttpResponse response = sendCurlToRouterHttpAPI(curlCommandToConnectToRouters.toString());
        assertEquals(200, response.getStatusLine().getStatusCode());

        curlCommandToConnectToRouters.delete(0, curlCommandToConnectToRouters.length())
                                     .append("http://localhost:")
                                     .append(container.getMappedPort(httpPortToSecondRouter));

        // send get request to second router via http
        response = sendCurlToRouterHttpAPI(curlCommandToConnectToRouters.toString());
        assertEquals(200, response.getStatusLine().getStatusCode());

        // connect to first router via socket
        String result = connectToRouterViaSocket(url, container.getMappedPort(portToFirstRouter));
        assertFalse(result.isEmpty());
        assertTrue(result.contains("Tarantool"));

        // connect to second router via socket
        result = connectToRouterViaSocket(url, container.getMappedPort(portToSecondRouter));
        assertFalse(result.isEmpty());
        assertTrue(result.contains("Tarantool"));

        // Connect to random port
        result = connectToRouterViaSocket(url, ThreadLocalRandom.current().nextInt(49152, 65535));
        assertTrue(result.isEmpty());
    }

    private HttpResponse sendCurlToRouterHttpAPI(String url) throws IOException {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpGet httpGetRequest = new HttpGet(url);
            return httpClient.execute(httpGetRequest);
        }
    }

    private String connectToRouterViaSocket(String url, int port) {
        final String returnedString;

        try (Socket socket = new Socket(url, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            returnedString = in.readLine();
        } catch (IOException e) {
            return "";
        }
        return returnedString;
    }
}
