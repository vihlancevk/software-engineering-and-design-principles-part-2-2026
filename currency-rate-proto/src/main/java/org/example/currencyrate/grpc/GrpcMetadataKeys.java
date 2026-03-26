package org.example.currencyrate.grpc;

import io.grpc.Metadata;

/** gRPC metadata key constants shared between client and server interceptors. */
public final class GrpcMetadataKeys {

    /** Carries the calling client's application name; set by the client, read by the server. */
    public static final Metadata.Key<String> CLIENT_NAME_KEY =
            Metadata.Key.of("x-client-name", Metadata.ASCII_STRING_MARSHALLER);

    private GrpcMetadataKeys() {}
}
