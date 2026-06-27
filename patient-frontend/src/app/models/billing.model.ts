export interface ChargeRequest {
  treatmentId: string;
  price: number;
}

export interface ChargeResponse {
  id: string;
  treatmentName: string;
  category: string;
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
