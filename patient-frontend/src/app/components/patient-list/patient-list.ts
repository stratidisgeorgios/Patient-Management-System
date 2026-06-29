import { Component, OnInit, computed, signal } from "@angular/core";
import { PatientResponse } from "../../models/patient.model";
import { PatientService } from "../../services/patient-service";
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';
import { ConfirmModal } from "../../shared/confirm-modal/confirm-modal";
import { NotificationService } from "../../services/notification-service";

const PAGE_SIZE = 30;

@Component({
  selector: "app-patient-list",
  imports: [ReactiveFormsModule, ConfirmModal],
  standalone: true,
  templateUrl: "./patient-list.html",
  styleUrl: "./patient-list.css",
})
export class PatientList implements OnInit {
  patients = signal<PatientResponse[]>([]);
  selectedPatient = signal<PatientResponse | null>(null);
  currentPage = signal(0);

  pagedPatients = computed(() =>
    this.patients().slice(this.currentPage() * PAGE_SIZE, (this.currentPage() + 1) * PAGE_SIZE)
  );
  totalPages = computed(() => Math.ceil(this.patients().length / PAGE_SIZE));

  editForm: FormGroup = new FormGroup({
    name: new FormControl('', [Validators.required, Validators.maxLength(100)]),
    email: new FormControl('', [Validators.required, Validators.email]),
    gender: new FormControl('', [Validators.required]),
    address: new FormControl('', [Validators.required, Validators.maxLength(200)]),
    dateOfBirth: new FormControl('', [Validators.required])
  });
  createForm: FormGroup = new FormGroup({
    name: new FormControl('', [Validators.required, Validators.maxLength(100)]),
    email: new FormControl('', [Validators.required, Validators.email]),
    gender: new FormControl('', [Validators.required]),
    address: new FormControl('', [Validators.required, Validators.maxLength(200)]),
    dateOfBirth: new FormControl('', [Validators.required]),
    registeredDate: new FormControl('', [Validators.required])
  });
  showCreateModal = signal(false);
  showEditModal = signal(false);
  showDeleteModal = signal(false);

  constructor(private patientService: PatientService, private notificationService: NotificationService) {}

  ngOnInit() {
    this.patientService.getAll().subscribe({
      next: (data: any) => {
        this.patients.set(data.sort((a: PatientResponse, b: PatientResponse) => a.name.localeCompare(b.name)));
      },
      error: (err) => {
        this.notificationService.error("Failed to load patients: " + err.message);
      }
    });
  }

  private closeAllModals() {
    this.showCreateModal.set(false);
    this.showEditModal.set(false);
    this.showDeleteModal.set(false);
    this.selectedPatient.set(null);
  }

  createPatient() {
    this.closeAllModals();
    this.showCreateModal.set(true);
    this.createForm.reset();
    this.createForm.patchValue({
      registeredDate: new Date().toISOString().split('T')[0]
    });
  }

  submitCreate() {
    this.patientService.create(this.createForm.value).subscribe((newPatient: any) => {
      this.patients.set([...this.patients(), newPatient].sort((a: PatientResponse, b: PatientResponse) => a.name.localeCompare(b.name)));
      this.showCreateModal.set(false);
      this.notificationService.success("Patient created successfully!");
    });
  }

  updatePatient(patient: PatientResponse) {
    this.closeAllModals();
    this.selectedPatient.set(patient);
    this.showEditModal.set(true);
    this.editForm.patchValue({
      name: patient.name,
      email: patient.email,
      gender: patient.gender,
      address: patient.address,
      dateOfBirth: patient.dateOfBirth
    });
  }

  submitUpdate() {
    this.patientService.update(this.selectedPatient()!.id, this.editForm.value).subscribe((updatedPatient: any) => {
      const index = this.patients().findIndex((p) => p.id === updatedPatient.id);
      if (index !== -1) {
        const updatedPatients = [...this.patients()];
        updatedPatients[index] = updatedPatient;
        this.patients.set(updatedPatients);
      }
      this.showEditModal.set(false);
      this.notificationService.success('Patient updated successfully');
    });
  }

  deletePatient(patient: PatientResponse) {
    this.closeAllModals();
    this.selectedPatient.set(patient);
    this.showDeleteModal.set(true);
  }

  submitDelete() {
    const id = this.selectedPatient()!.id;
    this.patientService.delete(id).subscribe(() => {
      this.patients.set(this.patients().filter((p) => p.id !== id));
      this.showDeleteModal.set(false);
      this.notificationService.success('Patient deleted successfully');
      if (this.currentPage() >= this.totalPages()) this.currentPage.set(Math.max(0, this.totalPages() - 1));
    });
  }

  cancelDelete() {
    this.showDeleteModal.set(false);
    this.selectedPatient.set(null);
  }

  prevPage() { if (this.currentPage() > 0) this.currentPage.update(p => p - 1); }
  nextPage() { if (this.currentPage() < this.totalPages() - 1) this.currentPage.update(p => p + 1); }
}
