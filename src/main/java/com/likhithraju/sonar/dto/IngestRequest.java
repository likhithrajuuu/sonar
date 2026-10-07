package com.likhithraju.sonar.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.JsonNode;

public record IngestRequest(
    @JsonProperty("device_id")
    String deviceId,
    List<JsonNode> events
) {
    
}
