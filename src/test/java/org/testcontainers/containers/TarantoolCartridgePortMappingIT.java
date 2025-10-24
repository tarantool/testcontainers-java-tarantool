package org.testcontainers.containers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TarantoolCartridgePortMappingIT {

    @Container
    private final static TarantoolCartridgeContainer container = new TarantoolCartridgeContainer(
            "Dockerfile",
            System.getenv().getOrDefault("TESTCONTAINERS_HUB_IMAGE_NAME_PREFIX", "") + "mapping-ports-container",
            "cartridge/instances.yml",
            "cartridge/replicasets.yml")
            .withEnv(TarantoolCartridgeContainer.ENV_TARANTOOL_CLUSTER_COOKIE, "secret")
            .withRouterUsername("admin")
            .withRouterPassword("secret")
            .withStartupTimeout(Duration.ofMinutes(5))
            .withLogConsumer(new Slf4jLogConsumer(
                    LoggerFactory.getLogger(TarantoolCartridgeBootstrapFromYamlIT.class)));

    @Test
    void portMappingTest() throws IOException, URISyntaxException {

        final int httpPortToFirstRouter = 8081;
        final int httpPortToSecondRouter = 8082;
        final int portToFirstRouter = 3301;
        final int portToSecondRouter = 3302;
        final String host = "localhost";
        final String schema = "http";

        container.addExposedPorts(httpPortToFirstRouter, httpPortToSecondRouter, portToFirstRouter, portToSecondRouter);
        container.start();

        URI firstRouterConnectionURI = new URI(schema, null, host,
                container.getMappedPort(httpPortToFirstRouter), null, null, null);

        // send get request to first router via http
        ClassicHttpResponse response = sendCurlToRouterHttpAPI(firstRouterConnectionURI);
        assertEquals(200, response.getCode());

        URI secondRouterConnectionURI = new URI(schema, null, host,
                container.getMappedPort(httpPortToSecondRouter), null, null, null);
        // send get request to second router via http
        response = sendCurlToRouterHttpAPI(secondRouterConnectionURI);
        assertEquals(200, response.getCode());

        // connect to first router via socket
        URI firstRouterConnectionURIViaSocket = new URI(null, null, host,
                container.getMappedPort(portToFirstRouter), null, null, null);
        String result = connectToRouterViaSocket(firstRouterConnectionURIViaSocket);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("Tarantool"));

        // connect to second router via socket
        URI secondRouterConnectionURIViaSocket = new URI(null, null, host,
                container.getMappedPort(portToSecondRouter), null, null, null);
        result = connectToRouterViaSocket(secondRouterConnectionURIViaSocket);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("Tarantool"));

        // Connect to random port
        URI randomPortConnectionURIViaSocket = new URI(null, null, host,
                ThreadLocalRandom.current().nextInt(49152, 65535), null, null, null);
        result = connectToRouterViaSocket(randomPortConnectionURIViaSocket);
        assertTrue(result.isEmpty());
    }

    private ClassicHttpResponse sendCurlToRouterHttpAPI(URI uri) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGetRequest = new HttpGet(uri);
            return httpClient.execute(httpGetRequest);
        }
    }

    private String connectToRouterViaSocket(URI uri) {
        final String returnedString;

        try (Socket socket = new Socket(uri.getHost(), uri.getPort());
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            returnedString = in.readLine();
        } catch (IOException e) {
            return "";
        }
        return returnedString;
    }
}
