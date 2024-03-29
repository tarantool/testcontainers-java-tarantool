package org.testcontainers.containers;

import java.io.File;
import java.util.Collections;
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
}
