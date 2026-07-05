package com.patientsystem.treatmentservice.dto;

public class TreatmentRequestDTO {
    private String name;
    private String category;
    private String price;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }
}
