package com.yuhaopro.acp.data.process;

import lombok.Data;

/**
 * {
 *  uid:
 *  key:
 * comment:
 * value:
 * }
 */
@Data
public class KafkaPOJO {
    private String uid;
    private String key;
    private String comment;
    private float value;
}

