export interface ChargeRequest {
  treatmentId: string;
}

export interface ChargeResponse {
  id: string;
  treatmentId: string;
  treatmentName: string;
  treatmentCategory: string;
  price: number;
  timestamp: string;
}

export interface BillingResponse {
  patientId: string;
  patientName: string;
  patientEmail: string;
  balance: number;
  charges: ChargeResponse[];
}
