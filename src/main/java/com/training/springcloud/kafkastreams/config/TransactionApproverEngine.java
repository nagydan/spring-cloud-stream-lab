package com.training.springcloud.kafkastreams.config;

import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;

import com.google.protobuf.Message;
import com.training.springcloud.kafkastreams.Approval;
import com.training.springcloud.kafkastreams.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.kafka.support.KafkaHeaders;

import java.util.*;
import java.util.function.Function;

@Configuration
public class TransactionApproverEngine {

    Map<String, Long> ledger = new HashMap<>();
    Set<String> blockedAccounts = new HashSet<>();

    @Bean
    public Function<DynamicMessage, ?> approve() {
        return trx -> {
            try {
                var transaction = Request.TransactionRequest.parseFrom(trx.toByteArray());
                if(!ledger.containsKey(transaction.getAccountId())) {
                    ledger.put(transaction.getAccountId(), 0L);
                }
                if (blockedAccounts.contains(transaction.getAccountId())) {
                    return Approval.TransactionApproval.newBuilder().setId(UUID.randomUUID().toString())
                            .setApprovedTransactionId(transaction.getId())
                            .setTransactionResult(Approval.TransactionResult.DECLINED)
                            .build();
                }
                var result = calcApproval(transaction, ledger.get(transaction.getAccountId()));
                return MessageBuilder
                        .withPayload(result)
                        .setHeader(KafkaHeaders.KEY, transaction.getId())
                        .build();
            }

            catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }
        };
    }


    private Approval.TransactionApproval calcApproval(Request.TransactionRequest transaction, Long currentBalance) {
        if (transaction.getTransactionType().equals(Request.TransactionType.CARD)
                || transaction.getTransactionType().equals(Request.TransactionType.DOMESTIC_BANK_TRANSFER)) {
            if(transaction.getTransactionDirection().equals(Request.TransactionDirection.CREDIT)) {
                var newBalance = ledger.get(transaction.getAccountId()) + transaction.getAmount();
                ledger.put(transaction.getAccountId(), newBalance);
                return Approval.TransactionApproval.newBuilder()
                        .setId(UUID.randomUUID().toString())
                        .setApprovedTransactionId(transaction.getId())
                        .setAccountId(transaction.getAccountId())
                        .setBalance(ledger.get(transaction.getAccountId()))
                        .setAmount(transaction.getAmount())
                        .setTransactionResult(Approval.TransactionResult.APPROVED)
                        .build();

            } else if (transaction.getTransactionDirection().equals(Request.TransactionDirection.DEBIT)) {
                if(currentBalance - transaction.getAmount() > 0 ) {
                    ledger.put(transaction.getAccountId(), currentBalance - transaction.getAmount());
                    return Approval.TransactionApproval.newBuilder()
                            .setId(UUID.randomUUID().toString())
                            .setApprovedTransactionId(transaction.getId())
                            .setAccountId(transaction.getAccountId())
                            .setBalance(ledger.get(transaction.getAccountId()))
                            .setAmount(transaction.getAmount())
                            .setTransactionResult(Approval.TransactionResult.APPROVED)
                            .build();
                } else {
                    return Approval.TransactionApproval.newBuilder()
                            .setId(UUID.randomUUID().toString())
                            .setApprovedTransactionId(transaction.getId())
                            .setAccountId(transaction.getAccountId())
                            .setBalance(ledger.get(transaction.getAccountId()))
                            .setAmount(transaction.getAmount())
                            .setTransactionResult(Approval.TransactionResult.DECLINED)
                            .build();
                }
            }
        } else if (transaction.getTransactionType().equals(Request.TransactionType.BLOCK)) {
            blockedAccounts.add(transaction.getAccountId());
            return Approval.TransactionApproval.newBuilder()
                    .setId(UUID.randomUUID().toString())
                    .setApprovedTransactionId(transaction.getId())
                    .setAccountId(transaction.getAccountId())
                    .setBalance(ledger.get(transaction.getAccountId()))
                    .setAmount(transaction.getAmount())
                    .setTransactionResult(Approval.TransactionResult.DECLINED)
                    .build();
        } else if (transaction.getTransactionType().equals(Request.TransactionType.UNBLOCK)) {
            if ( blockedAccounts.contains(transaction.getAccountId())) {
                blockedAccounts.remove(transaction.getAccountId());
                return Approval.TransactionApproval.newBuilder()
                        .setId(UUID.randomUUID().toString())
                        .setApprovedTransactionId(transaction.getId())
                        .setAccountId(transaction.getAccountId())
                        .setBalance(ledger.get(transaction.getAccountId()))
                        .setAmount(transaction.getAmount())
                        .setTransactionResult(Approval.TransactionResult.DECLINED)
                        .build();
            } else {
                throw new RuntimeException("UNEXPECTED MESSAGE");
            }
        }
        throw new RuntimeException("UNEXPECTED TYPE");
    }
}