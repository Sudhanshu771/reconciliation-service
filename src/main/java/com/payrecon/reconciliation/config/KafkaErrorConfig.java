package com.payrecon.reconciliation.config;

import com.payrecon.reconciliation.event.PaymentCreatedEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

// Same idea as payment-service's KafkaErrorConfig:
//   failure → retry 2 more times, 2 s apart → still failing → send to "payment.created-dlt"
@Configuration
public class KafkaErrorConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {

        DeadLetterPublishingRecoverer sendToDlt = new DeadLetterPublishingRecoverer(dltTemplate(bootstrapServers));
        FixedBackOff retryRule = new FixedBackOff(2000L, 2);

        DefaultErrorHandler handler = new DefaultErrorHandler(sendToDlt, retryRule);
        handler.addNotRetryableExceptions(IllegalArgumentException.class);   // bad data → no retries
        return handler;
    }

    // NEW: a small sender used ONLY for the DLT.
    // This service only CONSUMES, so it has no JSON producer of its own.
    // The DLT receives two kinds of values:
    //   - a PaymentCreatedEvent (the listener failed)          → send it as JSON
    //   - raw byte[] (the message wasn't even valid JSON)      → send the bytes as they are
    private KafkaTemplate<Object, Object> dltTemplate(String bootstrapServers) {
        Map<Class<?>, Serializer<?>> byType = Map.of(
                PaymentCreatedEvent.class, new JacksonJsonSerializer<>(),
                byte[].class, new ByteArraySerializer());

        DefaultKafkaProducerFactory<Object, Object> factory = new DefaultKafkaProducerFactory<>(
                Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                       ProducerConfig.MAX_BLOCK_MS_CONFIG, 2000),
                new DelegatingByTypeSerializer(Map.of(String.class, new StringSerializer())),  // key
                new DelegatingByTypeSerializer(byType));                                       // value

        return new KafkaTemplate<>(factory);
    }
}
