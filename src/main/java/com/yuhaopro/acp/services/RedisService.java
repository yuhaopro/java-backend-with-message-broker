package com.yuhaopro.acp.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.yuhaopro.acp.data.RuntimeEnvironment;

import jakarta.annotation.PreDestroy;
import lombok.Getter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Service
@Getter
public class RedisService {
    private Logger logger = LoggerFactory.getLogger(RedisService.class);
    private final JedisPool pool;

    public RedisService(RuntimeEnvironment environment) {
        this.pool = new JedisPool(environment.getRedisHost(), environment.getRedisPort());
    }

    public void writeToJedis(String key, String value) {
        try (Jedis jedis = pool.getResource()) {
            jedis.set(key, value);
        } catch (Exception e) {
            logger.error("Failed to write to redis", e);
        }
    }

    public String readFromJedis(String key) {
        try (Jedis jedis = pool.getResource()) {
            String result = null;
            if (jedis.exists(key)) {
                result = jedis.get(key);
            }
            return result;
        } catch (Exception e) {
            logger.error("Failed to write to redis", e);
            return null;
        }
    }

    public void deleteFromJedis(String key) {
        try (Jedis jedis = pool.getResource()) {
            jedis.del(key);
        } catch (Exception e) {
            logger.error("Failed to delete from redis", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        pool.close();
    }
}
