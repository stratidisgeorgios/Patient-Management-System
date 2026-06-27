import { Component, OnInit } from "@angular/core";
import { PatientResponse } from "../../models/patient.model";
import { PatientService } from "../../services/patient-service";
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';
import {signal} from "@angular/core";
import { ConfirmModal } from "../../shared/confirm-modal/confirm-modal";
import { NotificationService } from "../../services/notification-service";
@Component({
  selector: "app-patient-list",
  imports: [ReactiveFormsModule, ConfirmModal],
  standalone: true,
  templateUrl: "./patient-list.html",
  styleUrl: "./patient-list.css",
})
export class PatientList implements OnInit {
  patients = signal<PatientResponse[]>([]);
  patientToEdit = signal<PatientResponse | null>(null);
  patientToDelete = signal<PatientResponse | null>(null);
  editForm: FormGroup = new FormGroup({
    name: new FormControl('', [Validators.required, Validators.maxLength(100)]),
    email: new FormControl('', [Validators.required, Validators.email]),
    address: new FormControl('', [Validators.required, Validators.maxLength(200)]),
    dateOfBirth: new FormControl('', [Validators.required])
  });
  createForm: FormGroup = new FormGroup({
    name: new FormControl('', [Validators.required, Validators.maxLength(100)]),
    email: new FormControl('', [Validators.required, Validators.email]),
    address: new FormControl('', [Validators.required, Validators.maxLength(200)]),
    dateOfBirth: new FormControl('', [Validators.required]),
    registeredDate: new FormControl('', [Validators.required])
  });
  showCreateModal = signal(false);
  showEditModal = signal(false);
  showDeleteModal = signal(false);
  constructor(private patientService: PatientService, private notificationService: NotificationService) {}
  ngOnInit() {
    this.patientService.getAll().subscribe((data: any) => {
      this.patients.set(data.sort((a: PatientResponse, b: PatientResponse) => a.name.localeCompare(b.name)));
    });
  }

  createPatient() {
    this.patientToEdit.set(null);
    this.showCreateModal.set(true);
    this.createForm.reset();
    this.createForm.patchValue({
      registeredDate: new Date().toISOString().split('T')[0]  // "2026-06-24"
    });
  }

  submitCreate() {
    this.patientService.create(this.createForm.value).subscribe((newPatient: any) => {
      this.patients.set([...this.patients(), newPatient].sort((a: PatientResponse, b: PatientResponse) => a.name.localeCompare(b.name)));
      this.showCreateModal.set(false);
      this.notificationService.success("Patient created successfully!");
    });
  }

  editPatient(patient: PatientResponse) {
    this.patientToEdit.set(patient);
    this.showEditModal.set(true);
    this.editForm.patchValue({
      name: patient.name,
      email: patient.email,
      address: patient.address,
      dateOfBirth: patient.dateOfBirth
    });
  }

  submitUpdate() {
    this.patientService.update(this.patientToEdit()!.id, this.editForm.value).subscribe((updatedPatient: any) => {
      const index = this.patients().findIndex((p) => p.id === updatedPatient.id);
      if (index !== -1) {
        const updatedPatients = [...this.patients()];
        updatedPatients[index] = updatedPatient;
        this.patients.set(updatedPatients);
      }
      this.patientToEdit.set(null);
      this.showEditModal.set(false);
      this.notificationService.success('Patient updated successfully');
    });
  }

    
  deletePatient(patient: PatientResponse) {
    this.patientToDelete.set(patient);
    this.showDeleteModal.set(true);
  }

  confirmDelete() {
    this.patientService.delete(this.patientToDelete()!.id).subscribe(() => {
      this.patients.set(this.patients().filter((p) => p.id !== this.patientToDelete()!.id));
      this.showDeleteModal.set(false);
      this.patientToDelete.set(null);
      this.notificationService.success('Patient deleted successfully');
    });
  }

  cancelDelete() {
    this.showDeleteModal.set(false);
    this.patientToDelete.set(null);
  }

}
