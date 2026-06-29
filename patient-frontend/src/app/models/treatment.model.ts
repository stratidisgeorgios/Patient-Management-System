export interface TreatmentRequest {
  name: string;
  category: string;
  price: number;
}

export interface CategoryRequest {
  name: string;
  description: string;
}

export interface CategoryResponse {
  id: string;
  name: string;
  description: string;
}

export interface TreatmentResponse {
  id: string;
  name: string;
  category: CategoryResponse;
  price: number;
}
