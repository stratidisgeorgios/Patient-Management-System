import { Injectable, Inject } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { APP_SERVICE_CONFIG, AppConfig } from "../app-config.interface";
import { Observable } from "rxjs";

@Injectable({ providedIn: "root" })
export class AnalyticsService {
  constructor(@Inject(APP_SERVICE_CONFIG) private config: AppConfig, private http: HttpClient) {}

  getActivePatients(): Observable<number> {
    return this.http.get<number>(`${this.config.apiUrl}/api/analytics/active-patients`);
  }

  getAverageAge(): Observable<number | null> {
    return this.http.get<number | null>(`${this.config.apiUrl}/api/analytics/average-age`);
  }

  getGenderDistribution(): Observable<Record<string, number>> {
    return this.http.get<Record<string, number>>(`${this.config.apiUrl}/api/analytics/gender-distribution`);
  }

  getPatientRegistrationsPerMonth(year: number): Observable<any[][]> {
    return this.http.get<any[][]>(`${this.config.apiUrl}/api/analytics/patient-registrations/${year}`);
  }

  getAnnualRevenue(year: number): Observable<string> {
    return this.http.get(`${this.config.apiUrl}/api/analytics/annual-revenue/${year}`, { responseType: 'text' });
  }

  getRevenuePerCategory(): Observable<any[][]> {
    return this.http.get<any[][]>(`${this.config.apiUrl}/api/analytics/revenue-per-category`);
  }

  getMostUsedTreatments(): Observable<any[][]> {
    return this.http.get<any[][]>(`${this.config.apiUrl}/api/analytics/most-used-treatments`);
  }
}
