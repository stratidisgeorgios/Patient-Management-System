import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { TreatmentRequest, TreatmentResponse } from "../models/catalog.model";
import { Observable } from "rxjs";

@Injectable({
  providedIn: "root",
})
export class CatalogService {
  constructor(private http: HttpClient) {}

  getCatalog(): Observable<TreatmentResponse[]> {
    return this.http.get<TreatmentResponse[]>(`/api/catalog`);
  }

  createTreatment(treatment: TreatmentRequest): Observable<void> {
    return this.http.post<void>(`/api/catalog`, treatment);
  }

  updateTreatment(treatmentId: string, treatment: TreatmentRequest): Observable<void> {
    return this.http.put<void>(`/api/catalog/${treatmentId}`, treatment);
  }

  deleteTreatment(treatmentId: string): Observable<void> {
    return this.http.delete<void>(`/api/catalog/${treatmentId}`);
  }
}
