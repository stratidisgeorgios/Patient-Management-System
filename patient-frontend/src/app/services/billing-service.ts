import { HttpClient } from "@angular/common/http";
import { Inject, Injectable } from "@angular/core";
import { BillingResponse, ChargeRequest } from "../models/billing.model";
import { Observable } from "rxjs";
import { APP_SERVICE_CONFIG, AppConfig } from "../app-config.interface";

@Injectable({
  providedIn: "root",
})
export class BillingService {
  constructor(@Inject(APP_SERVICE_CONFIG) private config: AppConfig, private http: HttpClient) {}

  getBillingInfo(patientId: string): Observable<BillingResponse> {
    return this.http.get<BillingResponse>(`${this.config.apiUrl}/api/billing/${patientId}`);
  }

  addCharge(patientId: string, charge: ChargeRequest): Observable<void> {
    return this.http.post<void>(`${this.config.apiUrl}/api/billing/${patientId}/charge`, charge);
  }

  removeCharge(patientId: string, chargeId: string): Observable<void> {
    return this.http.delete<void>(`${this.config.apiUrl}/api/billing/${patientId}/charge/${chargeId}`);
  }
}
