package org.testcontainers.containers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DockerClientBuilder;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Class for working with docker directly
 *
 * @author Oleg Kuznetsov
 */
class TarantoolContainerImageHelper {

    private static final DockerClient dockerClient = DockerClientBuilder.getInstance().build();

    private TarantoolContainerImageHelper() {
    }

    /**
     * Checks image for existing by name and build if it not exist
     *
     * @param imageParams parameters for building tarantool image
     * @return image name
     */
    static String getImage(TarantoolImageParams imageParams) {
        final String sdkVersion = imageParams.getSdkVersion();

        if (StringUtils.isEmpty(sdkVersion)) {
            throw new IllegalArgumentException("SDK version is null or empty!");
        }

        if (!hasImage(sdkVersion)) {
            buildImage(imageParams);
        }

        return sdkVersion;
    }

    /**
     * Builds image from parameters
     *
     * @param imageParams parameters for building tarantool image
     */
    private static void buildImage(TarantoolImageParams imageParams) {
        final String sdkVersion = imageParams.getSdkVersion();
        final String uri = System.getenv("URI");

        if (StringUtils.isEmpty(uri)) {
            throw new IllegalStateException("URI environment variable must be specified!");
        }

        dockerClient.buildImageCmd(imageParams.getDockerfile())
                .withTags(new HashSet<>(Collections.singletonList(sdkVersion)))
                .withBuildArg("SDK_VERSION", sdkVersion)
                .withBuildArg("URI", uri)
                .exec(new BuildImageResultCallback())
                .awaitImageId();
    }

    /**
     * Checks image for existing by name
     *
     * @param imageName image name for searching
     * @return true if image exist and false if not
     */
    private static boolean hasImage(String imageName) {
        final List<Image> images = dockerClient.listImagesCmd().exec();
        return images.stream()
                .map(Image::getRepoTags)
                .map(Arrays::asList)
                .flatMap(Collection::stream)
                .anyMatch(tag -> tag.equals(imageName + ":latest"));
    }
}
