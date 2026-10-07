package com.likhithraju.sonar.validation;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component 
public class EventValidator {
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
