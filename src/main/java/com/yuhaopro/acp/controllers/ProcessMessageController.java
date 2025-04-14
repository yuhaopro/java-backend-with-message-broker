package com.yuhaopro.acp.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.yuhaopro.acp.data.process.RequestBodyPOJO;
import com.yuhaopro.acp.services.ProcessMessageService;

@RestController()
public class ProcessMessageController {

    private static final Logger logger = LoggerFactory.getLogger(ProcessMessageController.class);
    private final ProcessMessageService processMessageService;

    public ProcessMessageController(ProcessMessageService processMessageService) {
        this.processMessageService = processMessageService;
    }

    @PostMapping("/processMessages")
    public void processMessages(@RequestBody RequestBodyPOJO requestBody) {
        logger.info("Begin Processing Messages...");
        processMessageService.processMessages(requestBody);
    }

}
