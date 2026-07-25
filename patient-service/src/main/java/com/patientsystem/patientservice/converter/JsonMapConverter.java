package com.patientsystem.patientservice.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Converter;
import java.util.Map;

@Converter
public class JsonMapConverter extends JsonConverter<Map<String, Object>> {
    @Override
    protected TypeReference<Map<String, Object>> typeReference() {
        return new TypeReference<>() {};
    }
}
