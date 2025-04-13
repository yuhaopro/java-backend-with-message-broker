package com.yuhaopro.acp.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.yuhaopro.acp.data.RuntimeEnvironment;

import jakarta.annotation.PreDestroy;
import lombok.Getter;
import redis.clients.jedis.JedisPool;

@Service
@Getter
public class RedisService {
    private Logger logger = LoggerFactory.getLogger(RedisService.class);
    private final JedisPool pool;

    public RedisService(RuntimeEnvironment environment) {
        this.pool = new JedisPool(environment.getRedisHost(), environment.getRedisPort());
    }

    @PreDestroy
    public void cleanup() {
        pool.close();
    }
}
