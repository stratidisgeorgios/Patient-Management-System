import { Injectable } from "@angular/core";
import { Inject } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { APP_SERVICE_CONFIG, AppConfig } from "../app-config.interface";
import { Observable, tap } from "rxjs";
import { ImportStartResponse, PatientRequest, PatientResponse, ImportJobStatus, PresignedUrlResponse } from "../models/patient.model";
@Injectable({
  providedIn: "root",
})
export class PatientService {
  constructor(@Inject(APP_SERVICE_CONFIG) private config: AppConfig, private http: HttpClient) {}
  create(patient: PatientRequest): Observable<PatientResponse> {
    return this.http.post<PatientResponse>(`${this.config.apiUrl}/api/patients/create`, patient).pipe(tap(response => console.log('Create Patient response:', response)));
  }
  delete(id:string): Observable<void> {
    return this.http.delete<void>(`${this.config.apiUrl}/api/patients/delete/${id}`).pipe(tap(response => console.log('Delete Patient response:', response)));
  }
  update(id:string,patient: Omit<PatientRequest, "registeredDate">): Observable<PatientResponse> {
    return this.http.put<PatientResponse>(`${this.config.apiUrl}/api/patients/update/${id}`, patient).pipe(tap(response => console.log('Update Patient response:', response)));
  }
  getById(id:string): Observable<PatientResponse> {
    return this.http.get<PatientResponse>(`${this.config.apiUrl}/api/patients/${id}`).pipe(tap(response => console.log('Get Patient by ID response:', response)));
  }
  getPresignedUrl(): Observable<PresignedUrlResponse> {
    return this.http.get<PresignedUrlResponse>(`${this.config.apiUrl}/api/patients/import/presigned-url`);
  }
  uploadToS3(presignedUrl: string, file: File, onProgress: (percent: number) => void): Promise<void> {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      xhr.open('PUT', presignedUrl);
      xhr.upload.onprogress = (e) => {
        if (e.lengthComputable) onProgress(Math.round((e.loaded / e.total) * 100));
      };
      xhr.onload = () => (xhr.status >= 200 && xhr.status < 300) ? resolve() : reject(new Error(`Upload failed: ${xhr.status}`));
      xhr.onerror = () => reject(new Error('Upload network error'));
      xhr.send(file);
    });
  }
  startImport(s3Key: string, mapping: Record<string, string>, totalRows: number): Observable<ImportStartResponse> {
    return this.http.post<ImportStartResponse>(`${this.config.apiUrl}/api/patients/import/start`, { s3Key, mapping, totalRows }).pipe(tap(response => console.log('Start Import response:', response)));
  }
  getImportJobStatus(jobId: string): Observable<ImportJobStatus> {
    return this.http.get<ImportJobStatus>(`${this.config.apiUrl}/api/patients/import/status/${jobId}`).pipe(tap(response => console.log('Get Import Job Status response:', response)));
  }
}
