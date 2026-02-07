package org.example.currencyrateprovider;

import java.util.concurrent.ThreadLocalRandom;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.example.currencyrate.grpc.CurrencyRateServiceGrpc;
import org.example.currencyrate.grpc.RateRequest;
import org.example.currencyrate.grpc.RateResponse;

@GrpcService
public class CurrencyRateGrpcService extends CurrencyRateServiceGrpc.CurrencyRateServiceImplBase {

    private static final double BASE_RATE = 92.0;

    @Override
    public void getRate(RateRequest request, StreamObserver<RateResponse> responseObserver) {
        double rate = BASE_RATE + ThreadLocalRandom.current().nextDouble(-5.0, 5.0);
        double rounded = Math.round(rate * 100.0) / 100.0;

        RateResponse response = RateResponse.newBuilder()
                .setPair("USDRUB")
                .setRate(rounded)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
