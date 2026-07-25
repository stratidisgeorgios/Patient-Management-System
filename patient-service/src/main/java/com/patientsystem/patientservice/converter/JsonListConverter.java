package com.patientsystem.patientservice.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Converter;
import java.util.List;
import java.util.Map;

@Converter
public class JsonListConverter extends JsonConverter<List<Map<String, Object>>> {
    @Override
    protected TypeReference<List<Map<String, Object>>> typeReference() {
        return new TypeReference<>() {};
    }
}
