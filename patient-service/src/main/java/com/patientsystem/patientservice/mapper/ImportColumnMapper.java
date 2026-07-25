package com.patientsystem.patientservice.mapper;

import org.springframework.stereotype.Component;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ImportColumnMapper {

    private static final Map<String, List<String>> ALIASES = Map.of(
        "name",           List.of("name", "fullname", "patientname", "nombre", "firstname", "lastname", "surname"),
        "email",          List.of("email", "emailaddress", "mail", "correo"),
        "dateOfBirth",    List.of("dateofbirth", "dob", "birthday", "birthdate"),
        "gender",         List.of("gender", "sex"),
        "address",        List.of("address", "addr", "direccion"),
        "registeredDate", List.of("registereddate", "joindate", "registrationdate")
    );

    public Map<String, String> mapHeaders(List<String> headers) {
        Map<String, String> mapping = new LinkedHashMap<>();
        for (String header : headers) {
            mapping.put(header, resolveField(header));
        }
        return mapping;
    }

    private String resolveField(String header) {
        String normalized = header.toLowerCase().replaceAll("[^a-z0-9]", "");
        for (Map.Entry<String, List<String>> entry : ALIASES.entrySet()) {
            if (entry.getValue().contains(normalized)) return entry.getKey();
        }
        return header;
    }
}
