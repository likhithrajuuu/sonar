package com.likhithraju.sonar.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.likhithraju.sonar.dto.EventError;
import com.likhithraju.sonar.dto.IngestResponse;
import com.likhithraju.sonar.entity.Event;
import com.likhithraju.sonar.repository.TelemetryRepository;
import com.likhithraju.sonar.validation.EventValidator;
import com.likhithraju.sonar.validation.InvalidEventException;

import tools.jackson.databind.JsonNode;

@Service
public class IngestService {
    private final EventValidator eventValidator;
    private final TelemetryRepository telemetryRepository;

    public IngestService(EventValidator eventValidator, TelemetryRepository telemetryRepository) {
        this.eventValidator = eventValidator;
        this.telemetryRepository = telemetryRepository;
    }

    /**
     * Ingests a batch of events for a given device.
     * Validates each event and categorizes them into valid events and errors.
     * Duplicates within the same batch are counted but only the first occurrence is accepted.
     * Valid events are then inserted into the telemetry repository, and duplicates from previous requests are also counted.
     * @param deviceId
     * @param rawEvents
     * @return
     */
    @Transactional 
    public IngestResponse ingest(String deviceId, List<JsonNode> rawEvents){
        long now = System.currentTimeMillis();
        List<Event> valid = new ArrayList<>();
        List<EventError> errors = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        int duplicates = 0;
        for(int i = 0;i<rawEvents.size();i++){
            try{
                Event event = eventValidator.validate(rawEvents.get(i), now);
                // Duplicate inside the same batch: first occurrence wins.
                if(seen.add(event.timestamp())){
                    valid.add(event);
                }
                else{
                    duplicates++;
                }
            } catch(InvalidEventException e){
                errors.add(new EventError(i, e.getMessage()));
            }
        }
        valid.sort(Comparator.comparingLong(Event::timestamp));
        int accepted = 0;
        if(!valid.isEmpty()){
            // 1 = inserted, 0 = row already existed (duplicate from an earlier request).
            for(int inserted : telemetryRepository.insertBatch(deviceId, valid)){
                if(inserted > 0){
                    accepted++;
                }
                else{
                    duplicates++;
                }
            }
        }

        return new IngestResponse(deviceId, accepted, duplicates, errors.size(), errors);
    }
}
