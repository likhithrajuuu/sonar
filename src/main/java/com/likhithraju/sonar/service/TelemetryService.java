package com.likhithraju.sonar.service;

import org.springframework.stereotype.Service;

import com.likhithraju.sonar.dto.SummaryResponse;
import com.likhithraju.sonar.repository.TelemetryRepository;

@Service
public class TelemetryService {
    private final TelemetryRepository telemetryRepository;

    public TelemetryService(TelemetryRepository telemetryRepository) {
        this.telemetryRepository = telemetryRepository;
    }

    public SummaryResponse summary(String deviceId, long from, long to) {
        return telemetryRepository.summarize(deviceId, from, to);
    }
}
