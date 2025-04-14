package com.yuhaopro.acp.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.yuhaopro.acp.services.TransformMessageService;
import com.yuhaopro.acp.data.transform.RequestBodyPOJO;

@RestController
public class TransformMessageController {
    private Logger logger = LoggerFactory.getLogger(TransformMessageController.class);
    private TransformMessageService transformMessageService;
    
    public TransformMessageController(TransformMessageService transformMessageService) {
        this.transformMessageService = transformMessageService;
    }

    @PostMapping("/transformMessages")
    public void transformMessages(@RequestBody RequestBodyPOJO requestBody) {
        logger.debug("Transforming Messages");
        transformMessageService.transformMessages(requestBody);
    }

}
