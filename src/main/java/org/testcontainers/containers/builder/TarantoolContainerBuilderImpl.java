package org.testcontainers.containers.builder;

import org.testcontainers.containers.DefaultTarantoolContainer;
import org.testcontainers.containers.DefaultTarantoolContainerImpl;
import org.testcontainers.containers.TarantoolContainerSettings;
import org.testcontainers.containers.TarantoolLogLevel;

import static org.testcontainers.containers.PathUtils.normalizePath;

public class TarantoolContainerBuilderImpl implements TarantoolContainerBuilder {

    private final TarantoolContainerSettings settings;

    public TarantoolContainerBuilderImpl() {
        this(new TarantoolContainerSettings());
    }

    public TarantoolContainerBuilderImpl(TarantoolContainerSettings settings) {
        this.settings = settings;
    }

    public TarantoolContainerBuilder withUseFixedPorts(boolean useFixedPorts) {
        this.settings.setUseFixedPorts(useFixedPorts);
        return this;
    }

    public TarantoolContainerBuilder withImageName(String imageName) {
        this.settings.setImageName(imageName);
        return this;
    }

    public TarantoolContainerBuilder withHost(String host) {
        this.settings.setHost(host);
        return this;
    }

    public TarantoolContainerBuilder withPort(int port) {
        this.settings.setPort(port);
        return this;
    }

    public TarantoolContainerBuilder withUsername(String username) {
        this.settings.setUsername(username);
        return this;
    }

    public TarantoolContainerBuilder withPassword(String password) {
        this.settings.setPassword(password);
        return this;
    }

    public TarantoolContainerBuilder withLogLevel(TarantoolLogLevel logLevel) {
        this.settings.setLogLevel(logLevel);
        return this;
    }

    public TarantoolContainerBuilder withMemtxMemory(Integer memtxMemory) {
        if (memtxMemory <= 0) {
            throw new IllegalArgumentException(
                    String.format("The specified memtx_memory value must be >= 0, but was %d", memtxMemory));
        }
        this.settings.setMemtxMemory(memtxMemory);
        return this;
    }

    public TarantoolContainerBuilder withDirectoryBinding(String directoryResourcePath) {
        this.settings.setDirectoryResourcePath(normalizePath(directoryResourcePath));
        return this;
    }

    public TarantoolContainerBuilder withInstanceDir(String instanceDir) {
        this.settings.setInstanceDir(instanceDir);
        return this;
    }

    public TarantoolContainerBuilder withScriptFileName(String scriptFileName) {
        this.settings.setScriptFileName(scriptFileName);
        return this;
    }

    public DefaultTarantoolContainer build() {
        return new DefaultTarantoolContainerImpl(settings);
    }
}
