package com.likhithraju.sonar.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.likhithraju.sonar.dto.IngestRequest;
import com.likhithraju.sonar.dto.IngestResponse;
import com.likhithraju.sonar.dto.SummaryResponse;
import com.likhithraju.sonar.service.IngestService;
import com.likhithraju.sonar.service.TelemetryService;

@RestController
@RequestMapping("/api/v1/telemetry")
public class TelemetryController {

    private static final int MAX_EVENTS = 500;

    private final IngestService ingestService;
    private final TelemetryService telemetryService;

    public TelemetryController(IngestService ingestService, TelemetryService telemetryService) {
        this.ingestService = ingestService;
        this.telemetryService = telemetryService;
    }

    /**
     * Ingests a batch of events for a given device.
     * Validates the request and delegates to the IngestService for processing.
     * Throws a ResponseStatusException with BAD_REQUEST status if validation fails.
     * @param request
     * @return
     */
    @PostMapping("/ingest")
    public IngestResponse ingest(@RequestBody IngestRequest request) {
        if (request.deviceId() == null || request.deviceId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "device_id is required");
        }
        if (request.events() == null || request.events().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "events must be a non-empty array");
        }
        if (request.events().size() > MAX_EVENTS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "events must contain at most " + MAX_EVENTS + " items");
        }
        return ingestService.ingest(request.deviceId(), request.events());
    }

    /**
     * Returns a summary of events for a given device within a specified time range.
     * `from` and `to` are inclusive timestamps in milliseconds since epoch.
     * Validates the request parameters and delegates to the TelemetryService for processing.
     * Throws a ResponseStatusException with BAD_REQUEST status if validation fails.
     * @param deviceId
     * @param from
     * @param to
     * @return
     */
    @GetMapping("/summary")
    public SummaryResponse summary(
        @RequestParam("device_id") String deviceId,
        @RequestParam("from") long from,
        @RequestParam("to") long to
    ) {
        if(deviceId.isBlank()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "device_id is required");
        }
        if(from > to){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be less than or equal to to");
        }

        return telemetryService.summary(deviceId, from, to);
    }
}
