package com.yuhaopro.acp.services;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.yuhaopro.acp.data.RuntimeEnvironment;

import lombok.Getter;

@Service
@Getter
public class RabbitMqService {
    private Logger logger = LoggerFactory.getLogger((RabbitMqService.class));
    private ConnectionFactory factory;

    public RabbitMqService(RuntimeEnvironment environment) {
        factory = new ConnectionFactory();
        factory.setHost(environment.getRabbitMqHost());
        factory.setPort(environment.getRabbitMqPort());
    }

    public Connection createConnection() throws IOException, TimeoutException {
        return factory.newConnection();
    }

}
