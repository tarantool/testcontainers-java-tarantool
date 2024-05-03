package org.testcontainers.containers;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Tarantool image parameters holder
 *
 * @author Oleg Kuznetsov
 */
public class TarantoolImageParams {

    private final String tag;
    private final File dockerfile;
    private final Map<String, String> buildArgs;

    /**
     * Custom constructor for tarantool image parameters
     *
     * @param tag        docker image tag
     * @param dockerfile dockerfile for building custom tarantool image
     */
    public TarantoolImageParams(String tag, File dockerfile) {
        this(tag, dockerfile, Collections.emptyMap());
    }

    /**
     * Custom constructor for tarantool image parameters
     *
     * @param tag        docker image tag
     * @param dockerfile dockerfile for building custom tarantool image
     * @param buildArgs  args for building docker image
     */
    public TarantoolImageParams(String tag, File dockerfile, Map<String, String> buildArgs) {
        this.tag = tag;
        this.dockerfile = dockerfile;
        this.buildArgs = buildArgs;
    }

    /**
     * Custom constructor for tarantool image parameters
     *
     * @param tag          docker image tag
     * @param dockerfile   dockerfile for building custom tarantool image
     * @param baseImageName default image name for building docker image
     */
    public TarantoolImageParams(String tag, File dockerfile, String baseImageName) {
        this(tag, dockerfile, extract(baseImageName));
    }
 
    /**
     * Getter for sdk version
     *
     * @return sdk version
     */
    public String getTag() {
        return tag;
    }

    /**
     * Getter for dockerfile
     *
     * @return dockerfile
     */
    public File getDockerfile() {
        return dockerfile;
    }

    /**
     * Getter for buildArgs
     *
     * @return dockerfile
     */
    Map<String, String> getBuildArgs() {
        return buildArgs;
    }

    /**
     * Extract build arguments from environment variables
     *
     * @param defaultImage default image name for building docker image
     * @return map of building arguments
     */
    private static Map<String, String> extract(String defaultImage) {
        final Map<String, String> buildArgs = new HashMap<>();
        String registry = System.getenv("TARANTOOL_REGISTRY");
        if (registry != null && !registry.isEmpty()) {
            buildArgs.put("IMAGE", registry.endsWith("/") ? registry + defaultImage : registry + "/" + defaultImage);
        }
        buildArgs.put("DOWNLOAD_HOST", System.getenv("DOWNLOAD_HOST"));
        buildArgs.put("SDK_PATH", System.getenv("SDK_PATH"));
        buildArgs.put("CLUSTER_SRC_DIR", "vshard_cluster");
        return buildArgs;
    }
}
