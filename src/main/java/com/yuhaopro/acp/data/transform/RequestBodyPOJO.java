package com.yuhaopro.acp.data.transform;

import lombok.Data;

@Data
public class RequestBodyPOJO {
    private String readQueue;
    private String writeQueue;
    private int messageCount; 
}
