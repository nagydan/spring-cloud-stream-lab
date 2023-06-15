package com.training.springcloud.kafkastreams.config;

import com.training.springcloud.kafkastreams.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.function.Supplier;

@Configuration
public class ProducerConfig {

    @Autowired
    private StreamBridge streamBridge;

    @GetMapping("/producer")
    public void delegateToSupplier() {
        streamBridge.send("transform-out-0", new SecureRandom().nextInt(100));
    }

    @Bean
    public Supplier<Message> transactionGenerator() {
        return () -> {
            SecureRandom r = new SecureRandom();
            var transaction = Request.TransactionRequest.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setAccountId("TestAccount")
                .setAmount(Math.abs(r.nextInt(1000)))
                .setUserId("100")
                .setTransactionDirection(r.nextBoolean() ? Request.TransactionDirection.CREDIT: Request.TransactionDirection.DEBIT)
                .setTransactionType(r.nextBoolean() ? Request.TransactionType.CARD: Request.TransactionType.DOMESTIC_BANK_TRANSFER)

                .build();
            return MessageBuilder
                .withPayload(transaction)
                .setHeader(KafkaHeaders.KEY, transaction.getId())
                .build();
        };
    }
}