package com.yuhaopro.acp.data.process;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class KafkaPOJO {
    private String uid;
    private String key;
    private String comment;
    private float value;
}
