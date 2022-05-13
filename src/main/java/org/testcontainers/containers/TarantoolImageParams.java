package org.testcontainers.containers;

import java.io.File;
import java.net.URISyntaxException;

/**
 * Tarantool image parameters holder
 *
 * @author Oleg Kuznetsov
 */
public class TarantoolImageParams {

    private final String sdkVersion;
    private final File dockerfile;

    /**
     * Basic constructor for tarantool image parameters
     *
     * @param sdkVersion version of tarantool sdk which will be downloaded from specified in env variables URI,
     *                   for example: tarantool-enterprise-bundle-2.8.3-21-g7d35cd2be-r470
     */
    public TarantoolImageParams(String sdkVersion) {
        this.sdkVersion = sdkVersion;
        try {
            this.dockerfile = new File(TarantoolImageParams.class.getClassLoader()
                    .getResource("sdk/Dockerfile").toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Can't access to Dockerfile for testcontainers");
        }
    }

    /**
     * Custom constructor for tarantool image parameters
     *
     * @param sdkVersion version of tarantool sdk which will be downloaded from specified in env variables URI,
     *                   for example: tarantool-enterprise-bundle-2.8.3-21-g7d35cd2be-r470
     * @param dockerfile dockerfile for building custom tarantool image
     */
    public TarantoolImageParams(String sdkVersion, File dockerfile) {
        this.sdkVersion = sdkVersion;
        this.dockerfile = dockerfile;
    }

    /**
     * Getter for sdk version
     *
     * @return sdk version
     */
    public String getSdkVersion() {
        return sdkVersion;
    }

    /**
     * Getter for dockerfile
     *
     * @return dockerfile
     */
    public File getDockerfile() {
        return dockerfile;
    }
}
