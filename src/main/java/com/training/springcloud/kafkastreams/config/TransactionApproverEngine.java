package com.training.springcloud.kafkastreams.config;

import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Configuration
public class TransactionApproverEngine {

    Map<String, Long> ledger = new HashMap<>();
    Set<String> blockedAccounts = new HashSet<>();


    //TODO: implement the approval function
}