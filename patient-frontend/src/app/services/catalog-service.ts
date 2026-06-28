import { HttpClient } from "@angular/common/http";
import { Inject, Injectable } from "@angular/core";
import { TreatmentRequest, TreatmentResponse } from "../models/catalog.model";
import { Observable } from "rxjs";
import { APP_SERVICE_CONFIG, AppConfig } from "../app-config.interface";

@Injectable({
  providedIn: "root",
})
export class CatalogService {
  constructor(@Inject(APP_SERVICE_CONFIG) private config: AppConfig, private http: HttpClient) {}

  getCatalog(): Observable<TreatmentResponse[]> {
    return this.http.get<TreatmentResponse[]>(`${this.config.apiUrl}/api/catalog`);
  }

  createTreatment(treatment: TreatmentRequest): Observable<void> {
    return this.http.post<void>(`${this.config.apiUrl}/api/catalog`, treatment);
  }

  updateTreatment(treatmentId: string, treatment: TreatmentRequest): Observable<void> {
    return this.http.put<void>(`${this.config.apiUrl}/api/catalog/${treatmentId}`, treatment);
  }

  deleteTreatment(treatmentId: string): Observable<void> {
    return this.http.delete<void>(`${this.config.apiUrl}/api/catalog/${treatmentId}`);
  }
}
