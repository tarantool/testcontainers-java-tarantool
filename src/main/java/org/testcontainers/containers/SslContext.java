package org.testcontainers.containers;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@Getter
@NoArgsConstructor(staticName = "getSslContext")
@AllArgsConstructor(staticName = "getSslContext")
@ToString(includeFieldNames=true)
@EqualsAndHashCode
public class SslContext {
    @NonNull
    private String keyFile;
    @NonNull
    private String certFile;

//    public SslContext(@NonNull String keyFile, @NonNull String certFile) {
//        if (keyFile.isEmpty()) {
//            throw new RuntimeException("Parameter keyFile can not be empty String");
//        }
//        if (certFile.isEmpty()) {
//            throw new RuntimeException("Parameter certFile can not be empty String");
//        }
//
//        this.keyFile = keyFile;
//        this.certFile = certFile;
//    }
}
