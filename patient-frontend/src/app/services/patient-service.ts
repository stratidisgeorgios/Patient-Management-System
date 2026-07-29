import { Injectable } from "@angular/core";
import { Inject } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { APP_SERVICE_CONFIG, AppConfig } from "../app-config.interface";
import { Observable, firstValueFrom, tap } from "rxjs";
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
  uploadToS3(presignedUrl: string, file: File, onProgress: (loaded: number, total: number) => void): Promise<void> {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      xhr.open('PUT', presignedUrl);
      xhr.upload.onprogress = (e) => {
        if (e.lengthComputable) onProgress(e.loaded, e.total);
      };
      xhr.onload = () => (xhr.status >= 200 && xhr.status < 300) ? resolve() : reject(new Error(`Upload failed: ${xhr.status}`));
      xhr.onerror = () => reject(new Error('Upload network error'));
      xhr.send(file);
    });
  }
  private initiateMultipartUpload(): Observable<{ uploadId: string; s3Key: string }> {
    return this.http.post<{ uploadId: string; s3Key: string }>(`${this.config.apiUrl}/api/patients/import/multipart/initiate`, {});
  }

  private getPartPresignedUrl(s3Key: string, uploadId: string, partNumber: number): Observable<{ presignedUrl: string }> {
    return this.http.get<{ presignedUrl: string }>(`${this.config.apiUrl}/api/patients/import/multipart/part-url`, {
      params: { s3Key, uploadId, partNumber: partNumber.toString() }
    });
  }

  private completeMultipartUpload(s3Key: string, uploadId: string, parts: { partNumber: number; etag: string }[]): Observable<void> {
    return this.http.post<void>(`${this.config.apiUrl}/api/patients/import/multipart/complete`, { s3Key, uploadId, parts });
  }

  async uploadLargeFileToS3(file: File, onProgress: (loaded: number, total: number) => void): Promise<string> {
    const PART_SIZE = 50 * 1024 * 1024;
    const { uploadId, s3Key } = await firstValueFrom(this.initiateMultipartUpload());
    const totalParts = Math.ceil(file.size / PART_SIZE);
    const parts: { partNumber: number; etag: string }[] = [];
    let uploadedBytes = 0;

    for (let i = 0; i < totalParts; i++) {
      const partNumber = i + 1;
      const start = i * PART_SIZE;
      const end = Math.min(start + PART_SIZE, file.size);
      const chunk = file.slice(start, end);
      const { presignedUrl } = await firstValueFrom(this.getPartPresignedUrl(s3Key, uploadId, partNumber));

      const etag = await new Promise<string>((resolve, reject) => {
        const xhr = new XMLHttpRequest();
        xhr.open('PUT', presignedUrl);
        xhr.upload.onprogress = (e) => {
          if (e.lengthComputable) onProgress(uploadedBytes + e.loaded, file.size);
        };
        xhr.onload = () => {
          if (xhr.status >= 200 && xhr.status < 300) {
            uploadedBytes += end - start;
            resolve(xhr.getResponseHeader('ETag') ?? '');
          } else {
            reject(new Error(`Part ${partNumber} failed: ${xhr.status} ${xhr.responseText}`));
          }
        };
        xhr.onerror = () => reject(new Error(`Part ${partNumber} network error`));
        xhr.send(chunk);
      });

      parts.push({ partNumber, etag });
    }

    await firstValueFrom(this.completeMultipartUpload(s3Key, uploadId, parts));
    return s3Key;
  }

  startImport(s3Key: string, mapping: Record<string, string>, totalRows: number): Observable<ImportStartResponse> {
    return this.http.post<ImportStartResponse>(`${this.config.apiUrl}/api/patients/import/start`, { s3Key, mapping, totalRows }).pipe(tap(response => console.log('Start Import response:', response)));
  }
  getImportJobStatus(jobId: string): Observable<ImportJobStatus> {
    return this.http.get<ImportJobStatus>(`${this.config.apiUrl}/api/patients/import/status/${jobId}`).pipe(tap(response => console.log('Get Import Job Status response:', response)));
  }
}
