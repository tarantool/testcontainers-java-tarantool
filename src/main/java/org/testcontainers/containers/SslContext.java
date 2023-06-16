package org.testcontainers.containers;

public class SslContext {
    private String keyFile;
    private String certFile;

    private SslContext() {
    }

    private SslContext(String keyFile, String certFile) {
        this.keyFile = keyFile;
        this.certFile = certFile;
    }

    public static SslContext getSslContext() {
        return new SslContext();
    }

    public static SslContext getSslContext(String keyFile, String certFile) {
        return new SslContext(keyFile, certFile);
    }

    String getKeyFile() {
        return this.keyFile;
    }

    String getCertFile() {
        return this.certFile;
    }
}
