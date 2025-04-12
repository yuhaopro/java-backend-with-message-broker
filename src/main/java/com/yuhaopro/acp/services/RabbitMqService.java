package com.yuhaopro.acp.services;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.yuhaopro.acp.data.RuntimeEnvironment;

import jakarta.annotation.PreDestroy;
import lombok.Getter;

@Service
@Getter
public class RabbitMqService {
    private Logger logger = LoggerFactory.getLogger((RabbitMqService.class));
    private ConnectionFactory factory;
    private Connection connection;

    public RabbitMqService(RuntimeEnvironment environment) throws IOException, TimeoutException {
        factory = new ConnectionFactory();
        factory.setHost(environment.getRabbitMqHost());
        factory.setPort(environment.getRabbitMqPort());

        this.connection = createConnection();
    }

    public Connection createConnection() throws IOException, TimeoutException {
        return factory.newConnection();
    }

    public void writeToQueue(String queueName, byte[] messageBytes) {
        try (Channel channel = connection.createChannel()) {

            channel.queueDeclare(queueName, false, false, false, null);
            channel.basicPublish("", queueName, null, messageBytes);

        } catch (Exception e) {
            logger.error("Exception: ", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (this.connection != null && this.connection.isOpen()) {
                logger.info("Closing RabbitMQ connection...");
                this.connection.close();
                logger.info("RabbitMQ connection closed.");
            }
        } catch (IOException e) {
            logger.error("Error closing RabbitMQ connection: ", e);
        }
    }
}
