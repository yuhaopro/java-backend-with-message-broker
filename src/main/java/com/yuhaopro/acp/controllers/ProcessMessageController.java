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
import com.yuhaopro.acp.data.RuntimeEnvironment;
import com.yuhaopro.acp.data.processMessage.AcpStoragePOJO;
import com.yuhaopro.acp.data.processMessage.KafkaPOJO;
import com.yuhaopro.acp.data.processMessage.RabbitMqGoodPOJO;
import com.yuhaopro.acp.data.processMessage.RequestBodyPOJO;
import com.yuhaopro.acp.services.AcpStorageService;
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
    private final AcpStorageService acpStorageService;

    public ProcessMessageController(KafkaService kafkaService, RabbitMqService rabbitMqService,
            RuntimeEnvironment environment, AcpStorageService acpStorageService) {
        this.environment = environment;
        this.kafkaService = kafkaService;
        this.rabbitMqService = rabbitMqService;
        this.acpStorageService = acpStorageService;
    }

    @PostMapping("/processMessages")
    public void processMessages(@RequestBody RequestBodyPOJO requestBody) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        int recordCounter = 0;
        int runningTotalValue = 0;
        int totalGood = 0;
        int totalBad = 0;
        boolean keepConsuming = true;

        try (KafkaConsumer<String, String> consumer = kafkaService.createKafkaConsumer()) {
            consumer.subscribe(Collections.singletonList(requestBody.getReadTopic()));

            while (keepConsuming) {
                ConsumerRecords<String, String> records = consumer
                        .poll(Duration.ofMillis(environment.getKafkaPollingTimeout()));
                if (records.isEmpty()) {
                    continue;
                }

                for (ConsumerRecord<String, String> singleRecord : records) {
                    if (recordCounter > requestBody.getMessageCount()) {
                        keepConsuming = false;
                    }

                    logger.info("Value: {}", singleRecord.value());
                    KafkaPOJO kafkaBody = objectMapper.readValue(singleRecord.value(), KafkaPOJO.class);
                    String key = kafkaBody.getKey();

                    // write good queue
                    if (key.length() == 3 || key.length() == 4) {
                        runningTotalValue += 1;

                        AcpStoragePOJO acpStorageData = new AcpStoragePOJO(kafkaBody.getUid(),
                                kafkaBody.getKey(),
                                kafkaBody.getComment(),
                                kafkaBody.getValue(),
                                runningTotalValue);
                        String uuid = acpStorageService.saveToStorage(null);

                        RabbitMqGoodPOJO rabbitMqGoodMessage = new RabbitMqGoodPOJO(
                                uuid,
                                kafkaBody.getUid(),
                                kafkaBody.getKey(),
                                kafkaBody.getComment(),
                                kafkaBody.getValue(),
                                runningTotalValue);
                        rabbitMqService.writeToQueue(requestBody.getWriteQueueGood(), null);
                    } else {

                    }

                    recordCounter += 1;

                }
            }
        }
    }

}
