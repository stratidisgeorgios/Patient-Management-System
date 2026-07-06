import { Inject, Injectable } from "@angular/core";
import { APP_SERVICE_CONFIG, AppConfig } from "../app-config.interface";
import { HttpClient } from "@angular/common/http";
import { Observable, tap } from "rxjs";
import { OrganizationRequest, OrganizationResponse } from "../models/organization.model";
@Injectable({
  providedIn: "root",
})
export class OrganizationService {
  constructor(@Inject(APP_SERVICE_CONFIG) private config: AppConfig, private http: HttpClient) {}
  create(organization: OrganizationRequest): Observable<OrganizationResponse> {
    return this.http.post<OrganizationResponse>(`${this.config.apiUrl}/api/organizations`, organization).pipe(tap(response => console.log('Create Organization response:', response)));
  }
}
