package com.likhithraju.sonar.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SummaryResponse(
    @JsonProperty ("device_id")
    String deviceId,
    long from,
    long to,
    @JsonProperty ("avg_speed_kmph")
    Double avgSpeedKmph,
    @JsonProperty ("max_accel_magnitude")
    Double maxAccelMagnitude,
    @JsonProperty ("event_count")
    int eventCount
) {
    
}
