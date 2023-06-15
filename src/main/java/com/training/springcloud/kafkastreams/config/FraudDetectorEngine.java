package com.training.springcloud.kafkastreams.config;

import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.training.springcloud.kafkastreams.Approval;
import com.training.springcloud.kafkastreams.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class FraudDetectorEngine {

    @Bean
    public Consumer<DynamicMessage> fraudDetector() {
        return appr -> {
            try {
                var approval = Approval.TransactionApproval.parseFrom(appr.toByteArray());
                System.out.println(approval);
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }
        };
    }
}
