package com.yuhaopro.acp.controllers;

import java.time.Duration;
import java.util.Collections;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuhaopro.acp.data.RuntimeEnvironment;
import com.yuhaopro.acp.data.process.AcpStoragePOJO;
import com.yuhaopro.acp.data.process.KafkaPOJO;
import com.yuhaopro.acp.data.process.RabbitMqGoodPOJO;
import com.yuhaopro.acp.data.process.RabbitMqTotalPOJO;
import com.yuhaopro.acp.data.process.RequestBodyPOJO;
import com.yuhaopro.acp.services.AcpStorageService;
import com.yuhaopro.acp.services.KafkaService;
import com.yuhaopro.acp.services.RabbitMqService;

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
        float totalGood = 0;
        float totalBad = 0;
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
                    KafkaPOJO kafkaData = objectMapper.readValue(singleRecord.value(), KafkaPOJO.class);
                    String key = kafkaData.getKey();

                    // write good queue
                    if (key.length() == 3 || key.length() == 4) {
                        runningTotalValue += 1;

                        AcpStoragePOJO acpStorageData = new AcpStoragePOJO(kafkaData, runningTotalValue);
                        String uuid = acpStorageService.postToStorage(acpStorageData);

                        RabbitMqGoodPOJO rabbitMqGoodData = new RabbitMqGoodPOJO(acpStorageData, uuid);
                        String rabbitMqGoodMessage = objectMapper.writeValueAsString(rabbitMqGoodData);
                        rabbitMqService.writeToQueue(requestBody.getWriteQueueGood(), rabbitMqGoodMessage.getBytes());

                        totalGood += kafkaData.getValue();
                    } else {

                        String rabbitMqBadMessage = objectMapper.writeValueAsString(kafkaData);

                        rabbitMqService.writeToQueue(requestBody.getWriteQueueBad(), rabbitMqBadMessage.getBytes());
                        totalBad += kafkaData.getValue();
                    }

                    recordCounter += 1;

                }
            }
            
            RabbitMqTotalPOJO rabbitMqTotalGoodPOJO = new RabbitMqTotalPOJO(environment.getStudentNumber(), totalGood);
            RabbitMqTotalPOJO rabbitMqTotalBadPOJO = new RabbitMqTotalPOJO(environment.getStudentNumber(), totalBad);

            String rabbitMqTotalGoodMessage = objectMapper.writeValueAsString(rabbitMqTotalGoodPOJO);
            String rabbitMqTotalBadMessage = objectMapper.writeValueAsString(rabbitMqTotalBadPOJO);

            rabbitMqService.writeToQueue(requestBody.getWriteQueueGood(), rabbitMqTotalGoodMessage.getBytes());
            rabbitMqService.writeToQueue(requestBody.getWriteQueueBad(), rabbitMqTotalBadMessage.getBytes());
        }
    }

}
