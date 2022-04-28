package org.testcontainers.containers;

public class TarantoolContainerSettings {

    private String username;
    private String password;
    private String host;
    private Integer port;
    private boolean useFixedPorts;
    private TarantoolLogLevel logLevel;
    private Integer memtxMemory;
    private String directoryResourcePath;
    private String scriptFileName;
    private String instanceDir;
    private String imageName;

    public TarantoolContainerSettings() {
        this.username = "api_user";
        this.password = "secret";
        this.host = "localhost";
        this.port = 3301;
        this.useFixedPorts = false;
        this.logLevel = TarantoolLogLevel.VERBOSE;
        this.memtxMemory = 128 * 1024 * 1024; // 128 Mb in bytes;
        this.directoryResourcePath = "";
        this.scriptFileName = "server.lua";
        this.instanceDir = "/app";
        this.imageName = "tarantool/tarantool:2.x-centos7";
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public boolean isUseFixedPorts() {
        return useFixedPorts;
    }

    public void setUseFixedPorts(boolean useFixedPorts) {
        this.useFixedPorts = useFixedPorts;
    }

    public TarantoolLogLevel getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(TarantoolLogLevel logLevel) {
        this.logLevel = logLevel;
    }

    public Integer getMemtxMemory() {
        return memtxMemory;
    }

    public void setMemtxMemory(Integer memtxMemory) {
        this.memtxMemory = memtxMemory;
    }

    public String getDirectoryResourcePath() {
        return directoryResourcePath;
    }

    public void setDirectoryResourcePath(String directoryResourcePath) {
        this.directoryResourcePath = directoryResourcePath;
    }

    public String getScriptFileName() {
        return scriptFileName;
    }

    public void setScriptFileName(String scriptFileName) {
        this.scriptFileName = scriptFileName;
    }

    public String getInstanceDir() {
        return instanceDir;
    }

    public void setInstanceDir(String instanceDir) {
        this.instanceDir = instanceDir;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }
}
