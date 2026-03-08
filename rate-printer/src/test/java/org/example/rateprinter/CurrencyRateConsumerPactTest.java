package org.example.rateprinter;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.SynchronousMessagePactBuilder;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.V4Interaction;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.v4.MessageContents;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(
        providerName = "currency-rate-provider",
        providerType = ProviderType.SYNCH_MESSAGE
)
class CurrencyRateConsumerPactTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Pact(consumer = "rate-printer")
    V4Pact getRatePact(SynchronousMessagePactBuilder builder) {
        PactDslJsonBody request = new PactDslJsonBody();

        PactDslJsonBody response = new PactDslJsonBody()
                .stringType("pair", "USDRUB")
                .decimalType("rate", 92.0);

        return builder
                .expectsToReceive("get rate request")
                .withRequest(rb -> rb.withContent(request))
                .withResponse(rb -> rb.withContent(response))
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "getRatePact")
    void testGetRateContract(V4Interaction.SynchronousMessages message) throws Exception {
        List<MessageContents> response = message.getResponse();
        assertThat(response).hasSize(1);

        JsonNode body = MAPPER.readTree(response.get(0).getContents().valueAsString());

        assertThat(body.has("pair")).isTrue();
        assertThat(body.has("rate")).isTrue();
        assertThat(body.get("pair").asText()).isNotEmpty();
        assertThat(body.get("rate").asDouble()).isGreaterThan(0);
    }
}
