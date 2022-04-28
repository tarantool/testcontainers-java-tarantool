package org.testcontainers.containers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DockerClientBuilder;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class DockerClientUtils {

    private final static DockerClient dockerClient = DockerClientBuilder.getInstance().build();

    public static String getImage(String sdkVersion) {
        if (!hasImage(sdkVersion)) {
            buildImage(sdkVersion);
        }

        return sdkVersion;
    }

    public static void buildImage(String sdkVersion) {
        try {
            URI dockerFile = DockerClientUtils.class.getClassLoader()
                    .getResource("simple/Dockerfile").toURI();

            dockerClient.buildImageCmd(new File(dockerFile))
                    .withTags(new HashSet<>(Collections.singletonList(sdkVersion)))
                    .withBuildArg("SDK_VERSION", sdkVersion)
                    .withBuildArg("CREDS", System.getenv("CREDS"))
                    .exec(new BuildImageResultCallback())
                    .awaitImageId();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Can't access to Dockerfile for testcontainers");
        }
    }

    public static boolean hasImage(String imageName) {
        final List<Image> images = dockerClient.listImagesCmd().exec();
        return images.stream()
                .map(Image::getRepoTags)
                .map(Arrays::asList)
                .flatMap(Collection::stream)
                .anyMatch(tag -> tag.equals(imageName + ":latest"));
    }

    private DockerClientUtils() {
    }
}
