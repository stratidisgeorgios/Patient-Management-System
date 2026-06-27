import { Injectable } from "@angular/core";
import { Inject } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { APP_SERVICE_CONFIG, AppConfig } from "../app-config.interface";
import { tap } from "rxjs";
import { PatientRequest } from "../models/patient.model";
@Injectable({
  providedIn: "root",
})
export class PatientService {
  constructor(@Inject(APP_SERVICE_CONFIG) private config: AppConfig, private http: HttpClient) {}
  getAll(){
    return this.http.get(`/api/patients`).pipe(tap(response => console.log('Get All Patients response:', response)));
  }
  create(patient: PatientRequest){
    return this.http.post(`/api/patients/create`, patient).pipe(tap(response => console.log('Create Patient response:', response)));
  }
  delete(id:string){
    return this.http.delete(`/api/patients/delete/${id}`).pipe(tap(response => console.log('Delete Patient response:', response)));
  }
  update(id:string,patient: Omit<PatientRequest, "registeredDate">){
    return this.http.put(`/api/patients/update/${id}`, patient).pipe(tap(response => console.log('Update Patient response:', response)));
  }
}
