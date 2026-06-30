package com.patientsystem.treatmentservice.mapper;

import com.patientsystem.treatmentservice.dto.TreatmentResponseDTO;
import com.patientsystem.treatmentservice.model.Treatment;

public class TreatmentMapper {

    public static TreatmentResponseDTO toDTO(Treatment treatment) {
        TreatmentResponseDTO dto = new TreatmentResponseDTO();
        dto.setId(treatment.getId().toString());
        dto.setName(treatment.getName());
        dto.setPrice(treatment.getPrice());
        TreatmentResponseDTO.CategoryDTO categoryDTO = new TreatmentResponseDTO.CategoryDTO();
        categoryDTO.setId(treatment.getCategory().getId().toString());
        categoryDTO.setName(treatment.getCategory().getName());
        categoryDTO.setDescription(treatment.getCategory().getDescription());
        dto.setCategory(categoryDTO);
        return dto;
    }
}
