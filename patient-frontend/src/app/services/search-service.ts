import { HttpClient } from "@angular/common/http";
import { Inject, Injectable } from "@angular/core";
import { TreatmentSearchResponse, PatientSearchResponse } from "../models/search.model";
import { Observable } from "rxjs";
import { APP_SERVICE_CONFIG, AppConfig } from "../app-config.interface";

@Injectable({
  providedIn: "root",
})
export class SearchService {
  constructor(@Inject(APP_SERVICE_CONFIG) private config: AppConfig, private http: HttpClient) {}

  searchPatients(q:string): Observable<PatientSearchResponse[]> {
    return this.http.get<PatientSearchResponse[]>(`${this.config.apiUrl}/api/search/patients`, { params: { q } });
  }
  searchTreatments(q:string): Observable<TreatmentSearchResponse[]> {
    return this.http.get<TreatmentSearchResponse[]>(`${this.config.apiUrl}/api/search/treatments`, { params: { q } });
  }
}
