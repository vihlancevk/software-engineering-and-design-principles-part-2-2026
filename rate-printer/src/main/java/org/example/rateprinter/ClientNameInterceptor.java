package org.example.rateprinter;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import org.example.currencyrate.grpc.GrpcMetadataKeys;
import org.springframework.beans.factory.annotation.Value;

/**
 * Client-side gRPC interceptor that injects the application name into every outgoing
 * request as the {@code x-client-name} metadata header.
 *
 * <p>The server-side {@code MetricsServerInterceptor} reads this header to tag metrics
 * with the originating client, enabling "requests per second broken down by client"
 * queries in Grafana.
 */
@GrpcGlobalClientInterceptor
public class ClientNameInterceptor implements ClientInterceptor {

    private final String applicationName;

    public ClientNameInterceptor(@Value("${spring.application.name}") String applicationName) {
        this.applicationName = applicationName;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                headers.put(GrpcMetadataKeys.CLIENT_NAME_KEY, applicationName);
                super.start(responseListener, headers);
            }
        };
    }
}
