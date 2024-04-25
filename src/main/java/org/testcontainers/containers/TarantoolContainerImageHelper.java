package org.testcontainers.containers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.model.Image;
import org.testcontainers.DockerClientFactory;

/**
 * Class for working with docker directly
 *
 * @author Oleg Kuznetsov
 */
class TarantoolContainerImageHelper {

    private TarantoolContainerImageHelper() {
    }

    /**
     * Checks image for existing by name and build if it not exist
     *
     * @param imageParams parameters for building tarantool image
     * @return image name
     */
    static String getImage(TarantoolImageParams imageParams) {
        final String tag = imageParams.getTag();

        if (tag == null || tag.isEmpty()) {
            throw new IllegalArgumentException("Image tag is null or empty!");
        }

        if (!hasImage(tag)) {
            buildImage(imageParams);
        }

        return tag;
    }

    /**
     * Builds image from parameters
     *
     * @param imageParams parameters for building tarantool image
     */
    private static void buildImage(TarantoolImageParams imageParams) {
        final BuildImageCmd buildImageCmd = getDockerClient().buildImageCmd(imageParams.getDockerfile());

        final Map<String, String> buildArgs = imageParams.getBuildArgs();
        for (Map.Entry<String, String> entry : buildArgs.entrySet()) {
            buildImageCmd.withBuildArg(entry.getKey(), entry.getValue());
        }

        buildImageCmd.withTags(new HashSet<>(Collections.singletonList(imageParams.getTag())))
                .exec(new BuildImageResultCallback())
                .awaitImageId();
    }

    /**
     * Checks image for existing by name
     *
     * @param tag image tag for searching
     * @return true if image exist and false if not
     */
    private static boolean hasImage(String tag) {
        final List<Image> images = getDockerClient().listImagesCmd().exec();
        return images.stream()
                .map(Image::getRepoTags)
                .filter(Objects::nonNull)
                .map(Arrays::asList)
                .flatMap(Collection::stream)
                .anyMatch(repoTag -> repoTag.equals(tag));
    }


    private static DockerClient getDockerClient() {
        return DockerClientFactory.instance().client();
    }
}
