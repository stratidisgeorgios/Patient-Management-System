export interface PatientSearchResponse{
    id: string;
    name: string;
    email: string;
    dateOfBirth: string;
    gender: string;

}

export interface TreatmentSearchResponse {
    id: string;
    name: string;
    category: string;
    price: number;
}