package org.example.currencyrateprovider;

import au.com.dius.pact.provider.PactVerifyProvider;
import au.com.dius.pact.provider.junit5.MessageTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import org.example.currencyrate.grpc.RateRequest;
import org.example.currencyrate.grpc.RateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

@Provider("currency-rate-provider")
@PactBroker(url = "${PACT_BROKER_URL:http://localhost:9292}")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.cloud.zookeeper.enabled=false",
                "spring.cloud.zookeeper.discovery.enabled=false",
                "grpc.server.port=-1"
        }
)
class CurrencyRatePactProviderIT {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private CurrencyRateGrpcService grpcService;

    @BeforeEach
    void before(PactVerificationContext context) {
        context.setTarget(new MessageTestTarget());
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @PactVerifyProvider("get rate request")
    String verifyGetRate() throws JsonProcessingException {
        TestStreamObserver<RateResponse> observer = new TestStreamObserver<>();
        grpcService.getRate(RateRequest.newBuilder().build(), observer);
        RateResponse response = observer.getValue();

        String responseBody = MAPPER.writeValueAsString(Map.of(
                "pair", response.getPair(),
                "rate", response.getRate()
        ));
        return responseBody;
    }

    static class TestStreamObserver<T> implements StreamObserver<T> {

        private T value;

        @Override
        public void onNext(T value) {
            this.value = value;
        }

        @Override
        public void onError(Throwable t) {
            throw new RuntimeException(t);
        }

        @Override
        public void onCompleted() {
        }

        public T getValue() {
            return value;
        }
    }
}
