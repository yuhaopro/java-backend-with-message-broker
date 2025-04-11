package com.yuhaopro.acp.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yuhaopro.acp.data.ProcessMessageBody;
import com.yuhaopro.acp.data.RuntimeEnvironment;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController()
@RequestMapping("/")
public class ProcessMessageController {
    private static final Logger logger = LoggerFactory.getLogger(ProcessMessageController.class);
    private final RuntimeEnvironment environment;

    public ProcessMessageController(RuntimeEnvironment environment) {
        this.environment = environment;
    }

    @PostMapping("/processMessages")
    public void processMessages(@RequestBody ProcessMessageBody entity) {
        
    }
    
}
