package org.example.currencyrateprovider;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.example.currencyrate.grpc.GrpcMetadataKeys;

/**
 * Server-side gRPC interceptor that records per-request metrics tagged by calling client.
 *
 * <p>Metrics produced:
 * <ul>
 *   <li>{@code grpc.server.request.duration} (Timer) — tags: method, client, status.
 *       Histograms and percentiles are configured via
 *       {@code management.metrics.distribution.*} in application.properties.
 *       Enables histogram_quantile queries in Prometheus for any percentile.</li>
 *   <li>{@code grpc.server.errors} (Counter) — incremented for every non-OK response;
 *       tags: method, client, status. Used to count HTTP-500-equivalent failures.</li>
 * </ul>
 *
 * <p>The client name is read from the {@code x-client-name} gRPC metadata header sent by
 * {@link ClientNameInterceptor} on the client side. Falls back to {@code "unknown"}.
 */
@GrpcGlobalServerInterceptor
public class MetricsServerInterceptor implements ServerInterceptor {

    private static final String TAG_METHOD = "method";
    private static final String TAG_CLIENT = "client";
    private static final String TAG_STATUS = "status";

    private final MeterRegistry meterRegistry;

    public MetricsServerInterceptor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String client = headers.get(GrpcMetadataKeys.CLIENT_NAME_KEY);
        if (client == null || client.isBlank()) {
            client = "unknown";
        }
        String method = call.getMethodDescriptor().getBareMethodName();
        Timer.Sample sample = Timer.start(meterRegistry);
        final String resolvedClient = client;

        return next.startCall(
                new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
                    @Override
                    public void close(Status status, Metadata trailers) {
                        String statusCode = status.getCode().name();

                        // histogram_quantile and percentile gauges are controlled by
                        // management.metrics.distribution.* in application.properties
                        sample.stop(Timer.builder("grpc.server.request.duration")
                                .description("gRPC server-side request processing time")
                                .tag(TAG_METHOD, method)
                                .tag(TAG_CLIENT, resolvedClient)
                                .tag(TAG_STATUS, statusCode)
                                .register(meterRegistry));

                        if (!status.isOk()) {
                            meterRegistry.counter("grpc.server.errors",
                                    TAG_METHOD, method,
                                    TAG_CLIENT, resolvedClient,
                                    TAG_STATUS, statusCode)
                                    .increment();
                        }

                        super.close(status, trailers);
                    }
                },
                headers);
    }
}
