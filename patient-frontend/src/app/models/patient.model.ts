export interface PatientRequest {
  name: string;
  email: string;
  address: string;
  dateOfBirth: string;
  registeredDate: string;
  gender: string;
}

export interface PatientResponse {
  id: string;
  name: string;
  email: string;
  address: string;
  dateOfBirth: string;
  registeredDate: string;
  gender: string;
}

export interface ImportUploadResponse {
  s3Key: string;
  mapping: Record<string, string>;
  totalRows: number;
}

export interface PresignedUrlResponse {
  presignedUrl: string;
  s3Key: string;
}

export interface ImportStartResponse {
  importJobId: string;
}

export interface ImportJobStatus {
  status: string;
  importedRows: number;
  totalRows: number;
  failedRows: number;
  errors: { row: number; reason: string }[];
}