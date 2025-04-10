package com.yuhaopro.acp.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yuhaopro.acp.data.RuntimeEnvironment;

@RestController()
@RequestMapping("/kafka")
public class KafkaController {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaController.class);
    private final RuntimeEnvironment environment;

    public KafkaController(RuntimeEnvironment environment) {
        this.environment = environment;
    }

    @PutMapping("/{writeTopic}/{messageCount}")
    public void writeToKafkaTopic(@PathVariable String writeTopic, @PathVariable int messageCount) {
        
    }
    
}
