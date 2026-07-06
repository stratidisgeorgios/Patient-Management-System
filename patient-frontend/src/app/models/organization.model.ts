export interface OrganizationRequest {
  name: string;
  adminEmail: string;
}

export interface OrganizationResponse {
  id: string;
  name: string;
  adminEmail: string;
}