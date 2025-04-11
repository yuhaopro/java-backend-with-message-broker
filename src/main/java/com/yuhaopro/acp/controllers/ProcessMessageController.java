package com.yuhaopro.acp.controllers;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.yuhaopro.acp.data.ProcessMessageKafkaPOJO;
import com.yuhaopro.acp.data.ProcessMessageRequestPOJO;
import com.yuhaopro.acp.data.RuntimeEnvironment;
import com.yuhaopro.acp.services.KafkaService;
import com.yuhaopro.acp.services.RabbitMqService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController()
public class ProcessMessageController {

    private final RuntimeEnvironment environment;
    private static final Logger logger = LoggerFactory.getLogger(ProcessMessageController.class);
    private final KafkaService kafkaService;
    private final RabbitMqService rabbitMqService;

    public ProcessMessageController(KafkaService kafkaService, RabbitMqService rabbitMqService, RuntimeEnvironment environment, RuntimeEnvironment CurrentRuntimeEnvironment) {
        this.environment = environment;
        this.kafkaService = kafkaService;
        this.rabbitMqService = rabbitMqService;
    }

    @PostMapping("/processMessages")
    public void processMessages(@RequestBody ProcessMessageRequestPOJO requestBody) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        int recordCounter = 0;
        int runningTotalValue = 0;
        boolean keepConsuming = true;
        try (KafkaConsumer<String, String> consumer = kafkaService.createKafkaConsumer()) {
            consumer.subscribe(Collections.singletonList(requestBody.getReadTopic()));

            while (keepConsuming) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(environment.getKafkaPollingTimeout()));
                if (records.isEmpty()) {
                    continue;
                }

                for (ConsumerRecord<String, String> singleRecord : records) {
                    if (recordCounter > requestBody.getMessageCount()) {
                        keepConsuming = false;
                    }

                    logger.info("Value: {}", singleRecord.value());
                    ProcessMessageKafkaPOJO kafkaBody = objectMapper.readValue(singleRecord.value(), ProcessMessageKafkaPOJO.class);
                    String key = kafkaBody.getKey();
                    if (key.length() == 3 || key.length() == 4) {
                        runningTotalValue += 1;
                        try (Connection connection = rabbitMqService.createConnection();
                            Channel channel = connection.createChannel()) {

                            channel.queueDeclare(requestBody.getWriteQueueGood(), false, false, false, null);
                            channel.basicPublish("", requestBody.getWriteQueueGood(), null, jsonMessage.getBytes());

                        } catch (Exception e) {
                            logger.error("Exception: ", e);
                        }
                    } else {

                    }

                    recordCounter += 1;
                    
                }
            }
        }
    }

}
