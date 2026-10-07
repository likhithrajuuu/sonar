package com.likhithraju.sonar.validation;

import java.time.Duration;

import org.springframework.stereotype.Component;

import com.likhithraju.sonar.entity.Event;

import tools.jackson.databind.JsonNode;

@Component
public class EventValidator {

    private static final long MAX_FUTURE_MS = Duration.ofHours(24).toMillis();

    public Event validate(JsonNode node, long nowMs) throws InvalidEventException {
        if (node == null || !node.isObject()) {
            throw new InvalidEventException("event must be an object");
        }

        JsonNode ts = field(node, "timestamp");
        if (!ts.isIntegralNumber() || !ts.canConvertToLong()) {
            throw new InvalidEventException("timestamp must be an integer");
        }
        long timestamp = ts.longValue();
        if (timestamp <= 0) {
            throw new InvalidEventException("timestamp must be positive");
        }
        if (timestamp > nowMs + MAX_FUTURE_MS) {
            throw new InvalidEventException("timestamp more than 24 hours in the future");
        }

        double lat = number(node, "lat");
        if (lat < -90 || lat > 90) {
            throw new InvalidEventException("lat out of range");
        }
        double lon = number(node, "lon");
        if (lon < -180 || lon > 180) {
            throw new InvalidEventException("lon out of range");
        }
        double speed = number(node, "speed_kmph");
        if (speed < 0 || speed > 300) {
            throw new InvalidEventException("speed_kmph out of range");
        }

        return new Event(
            timestamp, lat, lon, speed,
            number(node, "accel_x"), number(node, "accel_y"), number(node, "accel_z"),
            number(node, "gyro_x"), number(node, "gyro_y"), number(node, "gyro_z"));
    }

    /**
     * Fetching a proper field name from the event node.
     * If the field is missing or null, an InvalidEventException is thrown.
     * @param node
     * @param name
     * @return
     * @throws InvalidEventException
     */
    private JsonNode field(JsonNode node, String name) throws InvalidEventException {
       JsonNode value = node.get(name);
       if(value == null || value.isNull()) {
           throw new InvalidEventException("Missing field: " + name);
       }
       return value;
    }

    /**
     * Fetching a numeric field from the event node.
     * If the field is missing, null, or not numeric, an InvalidEventException is
     * @param node
     * @param name
     * @return
     * @throws InvalidEventException
     */
    private double number(JsonNode node, String name) throws InvalidEventException {
        JsonNode value = field(node, name);
        if(!value.isNumber()){
            throw new InvalidEventException(name + " must be numeric");
        }
        return value.asDouble();
    }
}
