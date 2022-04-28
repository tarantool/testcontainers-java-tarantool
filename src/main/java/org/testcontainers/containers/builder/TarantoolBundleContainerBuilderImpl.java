package org.testcontainers.containers.builder;

import org.testcontainers.containers.DockerClientUtils;
import org.testcontainers.containers.TarantoolContainerSettings;

public class TarantoolBundleContainerBuilderImpl extends TarantoolContainerBuilderImpl
        implements TarantoolBundleContainerBuilder {

    private final static String DEFAULT_SDK = "tarantool-enterprise-bundle-2.10.0-beta2-59-gc51e2ba67-r469";

    public TarantoolBundleContainerBuilderImpl() {
        this(DEFAULT_SDK);
    }

    public TarantoolBundleContainerBuilderImpl(String sdkName) {
        super(makeSettingsWithBuiltImage(sdkName));
    }

    private static TarantoolContainerSettings makeSettingsWithBuiltImage(String sdkName) {
        final TarantoolContainerSettings settings = new TarantoolContainerSettings();
        settings.setImageName(DockerClientUtils.getImage(sdkName));
        settings.setUsername("test_user");
        settings.setPassword("test_password");
        return settings;
    }
}
