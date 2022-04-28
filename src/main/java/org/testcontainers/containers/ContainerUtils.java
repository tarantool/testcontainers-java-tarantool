package org.testcontainers.containers;

import org.testcontainers.images.builder.ImageFromDockerfile;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

public class ContainerUtils {

    private ContainerUtils() {
    }


    public static Future<String> imageWithArguments(ImageFromDockerfile image, List<String> variables,
                                                    final Map<String, String> buildArgs) {
        if (!buildArgs.isEmpty()) {
            image.withBuildArgs(buildArgs);
        }

        for (String envVariable : variables) {
            String variableValue = System.getenv(envVariable);
            if (variableValue != null) {
                image.withBuildArg(envVariable, variableValue);
            }
        }
        return image;
    }

    public static ImageFromDockerfile buildImage(String path, String dockerFile, String buildImageName) {
        if (buildImageName != null && !buildImageName.isEmpty()) {
            return new ImageFromDockerfile(buildImageName, false)
                    .withFileFromClasspath(path, dockerFile);
        }
        return buildImage(path, dockerFile);
    }

    public static ImageFromDockerfile buildImage(String path, String dockerFile) {
        return new ImageFromDockerfile().withFileFromClasspath(path, dockerFile);
    }
}
