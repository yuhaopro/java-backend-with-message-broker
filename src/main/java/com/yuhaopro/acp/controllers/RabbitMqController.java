package com.yuhaopro.acp.controllers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import com.yuhaopro.acp.data.RuntimeEnvironment;
import com.yuhaopro.acp.services.RabbitMqService;

/**
 * RabbitMqController is a REST controller that provides endpoints for sending
 * and receiving stock symbols
 * through RabbitMQ. This class interacts with a RabbitMQ environment which is
 * configured dynamically during runtime.
 */
@RestController()
@RequestMapping("/rabbitMq")
public class RabbitMqController {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMqController.class);
    private final RuntimeEnvironment environment;

    private final RabbitMqService rabbitMqService;

    public RabbitMqController(RuntimeEnvironment environment, RabbitMqService rabbitMqService) {
        this.environment = environment;
        this.rabbitMqService = rabbitMqService;
    }

    @PutMapping("/{queueName}/{messageCount}")
    public void sendMessageCount(@PathVariable String queueName, @PathVariable int messageCount) {
        logger.info("Writing {} symbols in queue {}", messageCount, queueName);
        try (Connection connection = rabbitMqService.createConnection();
                Channel channel = connection.createChannel()) {

            channel.queueDeclare(queueName, false, false, false, null);

            final String uuid = environment.getStudentNumber();
            for (Integer i = 0; i < messageCount; i++) {

                Map<String, String> data = new HashMap<>();
                data.put("uid", uuid);
                data.put("counter", i.toString());

                ObjectMapper objectMapper = new ObjectMapper();
                String jsonMessage = objectMapper.writeValueAsString(data);

                // using nameless exchange
                channel.basicPublish("", queueName, null, jsonMessage.getBytes());
                logger.info(" [x] Sent message: {} to queue: {}", jsonMessage, queueName);
            }

        } catch (Exception e) {
            logger.error("Exception: ", e);
        }
    }

    @GetMapping("/{queueName}/{timeoutInMsec}")
    public List<String> receiveMessageList(@PathVariable String queueName, @PathVariable int timeoutInMsec) {
        logger.info("Reading messages from queue {} with timeout {} ms", queueName, timeoutInMsec);
        List<String> result = new ArrayList<>();
        Instant startTime = Instant.now();
        Instant endTime = startTime.plus(Duration.ofMillis(timeoutInMsec));
        AtomicBoolean timeoutReached = new AtomicBoolean(false);

        try (Connection connection = rabbitMqService.createConnection();
             Channel channel = connection.createChannel()) {

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                if (!timeoutReached.get()) {
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    logger.info("{}:{} -> {}", queueName, delivery.getEnvelope().getRoutingKey(), message);
                    result.add(message);
                } else {
                    channel.basicCancel(consumerTag); //cancel the consumer if timeout is reached.
                }
            };
            String consumerTag = channel.basicConsume(queueName, true, deliverCallback, consumerTagLocal -> {});

            while (Instant.now().isBefore(endTime) && !timeoutReached.get()) {
                try {
                    Thread.sleep(100); // Check every 100ms to prevent high cpu usage during this spinlock
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("Interrupted while waiting for timeout", e);
                }
            }
            timeoutReached.set(true); // Signal timeout.
            channel.basicCancel(consumerTag); // To exit if the prev message hasn't finish processing.

            logger.info("done consuming events. {} record(s) received", result.size());

        } catch (Exception e) {
            logger.error("Error consuming messages", e);
        }

        return result;
    }
}