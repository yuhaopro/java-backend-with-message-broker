package com.yuhaopro.acp.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class ServiceController {
    @GetMapping("/")
    public String healthcheck() {
        return "<html>Service is working</html>";
    }

}
