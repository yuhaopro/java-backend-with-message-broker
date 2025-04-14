package com.yuhaopro.acp.data.transform;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
public class SpecialPOJO {
    private int totalMessagesWritten;
    private int totalMessagesProcessed;
    private int totalRedisUpdates;
    private double totalValueWritten;
    private double totalAdded;
}
