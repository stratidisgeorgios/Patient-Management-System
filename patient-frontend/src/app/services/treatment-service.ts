import { HttpClient } from "@angular/common/http";
import { Inject, Injectable } from "@angular/core";
import { TreatmentRequest, TreatmentResponse } from "../models/treatment.model";
import { Observable } from "rxjs";
import { APP_SERVICE_CONFIG, AppConfig } from "../app-config.interface";

@Injectable({
  providedIn: "root",
})
export class TreatmentService {
  constructor(@Inject(APP_SERVICE_CONFIG) private config: AppConfig, private http: HttpClient) {}

  getById(treatmentId: string): Observable<TreatmentResponse> {
    return this.http.get<TreatmentResponse>(`${this.config.apiUrl}/api/treatments/${treatmentId}`);
  }

  createTreatment(treatment: TreatmentRequest): Observable<TreatmentResponse> {
    const body = { ...treatment, price: String(treatment.price) };
    return this.http.post<TreatmentResponse>(`${this.config.apiUrl}/api/treatments`, body);
  }

  updateTreatment(treatmentId: string, treatment: TreatmentRequest): Observable<TreatmentResponse> {
    const body = { ...treatment, price: String(treatment.price) };
    return this.http.put<TreatmentResponse>(`${this.config.apiUrl}/api/treatments/${treatmentId}`, body);
  }

  deleteTreatment(treatmentId: string): Observable<void> {
    return this.http.delete<void>(`${this.config.apiUrl}/api/treatments/${treatmentId}`);
  }
}
