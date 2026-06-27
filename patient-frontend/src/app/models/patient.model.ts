export interface PatientRequest {
  name: string;
  email: string;
  address: string;
  dateOfBirth: string;
  registeredDate: string;
  gender: string;
}

export interface PatientResponse {
  id: string;
  name: string;
  email: string;
  address: string;
  dateOfBirth: string;
  registeredDate: string;
  gender: string;
}
