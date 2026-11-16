package com.sara.backend.exception;

import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ValidationMessageBuilder {

    public String buildInvalidFieldsMessage(List<FieldError> fieldErrors) {
        String fieldNames = fieldErrors.stream()
                .map(FieldError::getField)
                .distinct()
                .sorted()
                .collect(Collectors.joining(", "));
        return "Invalid value for: " + fieldNames + ".";
    }
}
