package com.patientsystem.searchservice.documents;
public class TreatmentDocument {
    private String treatmentId;
    private String name;
    private String category;
    private String price;

    public String getTreatmentId() { return treatmentId; }
    public void setTreatmentId(String treatmentId) { this.treatmentId = treatmentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }
}
