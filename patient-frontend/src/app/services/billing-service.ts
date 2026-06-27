import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { BillingResponse, ChargeRequest } from "../models/billing.model";
import { Observable } from "rxjs";

@Injectable({
  providedIn: "root",
})
export class BillingService {
  constructor(private http: HttpClient) {}

  getBillingInfo(patientId: string): Observable<BillingResponse> {
    return this.http.get<BillingResponse>(`/api/billing/${patientId}`);
  }

  addCharge(patientId: string, charge: ChargeRequest): Observable<void> {
    return this.http.post<void>(`/api/billing/${patientId}/charge`, charge);
  }
}
