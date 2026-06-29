import { Inject, Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { APP_SERVICE_CONFIG, AppConfig } from "../app-config.interface";
import { CategoryRequest, CategoryResponse } from "../models/treatment.model";
@Injectable({
  providedIn: "root",
})
export class CategoryService {
  constructor(@Inject(APP_SERVICE_CONFIG) private config: AppConfig, private http: HttpClient) {}


  getCategories(): Observable<CategoryResponse[]> {
    return this.http.get<CategoryResponse[]>(`${this.config.apiUrl}/api/categories`);
  }

  createCategory(category: CategoryRequest): Observable<CategoryResponse> {
    return this.http.post<CategoryResponse>(`${this.config.apiUrl}/api/categories`, category);
  }

  updateCategory(categoryId: string, category: CategoryRequest): Observable<CategoryResponse> {
    return this.http.put<CategoryResponse>(`${this.config.apiUrl}/api/categories/${categoryId}`, category);
  }

  deleteCategory(categoryId: string): Observable<void> {
    return this.http.delete<void>(`${this.config.apiUrl}/api/categories/${categoryId}`);
  } 
}
