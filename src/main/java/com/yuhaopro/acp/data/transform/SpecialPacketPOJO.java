package com.yuhaopro.acp.data.transform;

import lombok.Data;

@Data
public class SpecialPacketPOJO {
    private int totalMessagesWritten;
    private int totalMessagesProcessed;
    private int totalRedisUpdates;
    private float totalValueWritten;
    private float totalAdded;
}
