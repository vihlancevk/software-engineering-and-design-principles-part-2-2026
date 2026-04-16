package org.example.rateprinter;

import org.example.currencyrate.grpc.CurrencyRateServiceGrpc;
import org.example.currencyrate.grpc.RateRequest;
import org.example.currencyrate.grpc.RateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import net.devh.boot.grpc.client.inject.GrpcClient;

@Service
public class RatePrinterService {

    private static final Logger log = LoggerFactory.getLogger(RatePrinterService.class);

    @GrpcClient("currency-rate")
    private CurrencyRateServiceGrpc.CurrencyRateServiceBlockingStub stub;

    @Scheduled(fixedRate = 5000)
    public void printRate() {
        try {
            RateResponse response = stub.getRate(RateRequest.newBuilder().build());
            log.info("Current rate: {} = {}", response.getPair(), response.getRate());
        } catch (Exception e) {
            log.error("Failed to fetch rate: {}", e.getMessage());
        }
    }
}
