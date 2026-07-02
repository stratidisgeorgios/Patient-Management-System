import { Component, OnDestroy, OnInit, signal } from "@angular/core";
import { PatientResponse } from "../../models/patient.model";
import { PatientService } from "../../services/patient-service";
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';
import { ConfirmModal } from "../../shared/confirm-modal/confirm-modal";
import { NotificationService } from "../../services/notification-service";
import { SearchService } from "../../services/search-service";
import { Router } from "@angular/router";
import { catchError, debounceTime, forkJoin, map, of, Subject, switchMap, takeUntil } from "rxjs";
import { SseService } from "../../services/sse-service";
@Component({
  selector: "app-patient-list",
  imports: [ReactiveFormsModule, ConfirmModal],
  standalone: true,
  templateUrl: "./patient-list.html",
  styleUrl: "./patient-list.css",
})
export class PatientList implements OnInit, OnDestroy {
  patients = signal<PatientResponse[]>([]);
  selectedPatient = signal<PatientResponse | null>(null);

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
  searchQuery = new Subject<string>();
  hasSearched = signal(false);
  isLoading = signal(false);
  lastSearchQuery = '';
  private destroy$ = new Subject<void>();
  constructor(private patientService: PatientService, private searchService: SearchService, private notificationService: NotificationService, private router: Router, private sseService: SseService) {}

  ngOnInit() {
    this.sseService.connect().pipe(takeUntil(this.destroy$)).subscribe({
      next: (message) => {
      if (message === 'patient' && this.lastSearchQuery) {
          this.searchQuery.next(this.lastSearchQuery);
        }
      },
      error: (err) => {
        console.error("SSE connection error:", err);
      }
    });
    this.searchQuery.pipe(
      debounceTime(200),
      switchMap(value => {
        if (!value.trim()) {
          this.hasSearched.set(false);
          this.patients.set([]);
          this.isLoading.set(false);
          return of([]);
        }
        this.hasSearched.set(true);
        this.isLoading.set(true);
        this.lastSearchQuery = value;
        return this.searchService.searchPatients(value).pipe(
          switchMap(data => {
            if (data.length === 0) {
              return of([]);
            }
            return forkJoin(data.map(p =>
              this.patientService.getById(p.id).pipe(catchError(() => of(null)))
            )).pipe(map(results => results.filter(p => p !== null)));
          })
        );
      }), takeUntil(this.destroy$)
    ).subscribe({
      next: (fullPatients) => {
        this.patients.set(fullPatients.sort((a, b) => a.name.localeCompare(b.name)));
        this.isLoading.set(false);
      },
      error: (err) => {
        this.notificationService.error("Failed to search patients: " + this.extractError(err));
        this.isLoading.set(false);
      }
    });
  }
  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
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
    this.patientService.create(this.createForm.value).subscribe({
      next: () => {
        this.showCreateModal.set(false);
        this.notificationService.success("Patient created successfully!");
      },
      error: (err) => this.notificationService.error("Failed to create patient: " + this.extractError(err))
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
    this.patientService.update(this.selectedPatient()!.id, this.editForm.value).subscribe({
      next: () => {
        this.showEditModal.set(false);
        this.notificationService.success('Patient updated successfully');
      },
      error: (err) => this.notificationService.error("Failed to update patient: " + this.extractError(err))
    });
  }


  deletePatient(patient: PatientResponse) {
    this.closeAllModals();
    this.selectedPatient.set(patient);
    this.showDeleteModal.set(true);
  }
  
  submitDelete() {
    const id = this.selectedPatient()!.id;
    this.patientService.delete(id).subscribe({
      next: () => {
        this.showDeleteModal.set(false);
        this.notificationService.success('Patient deleted successfully');
      },
      error: (err) => this.notificationService.error("Failed to delete patient: " + this.extractError(err))
    });
  }

  cancelDelete() {
    this.showDeleteModal.set(false);
    this.selectedPatient.set(null);
  }

  private extractError(err: any): string {
    if (err.error?.message) return err.error.message;
    if (typeof err.error === 'string') return err.error;
    if (err.error && typeof err.error === 'object') return Object.values(err.error).join(', ');
    return err.message || 'An unexpected error occurred';
  }
    openProfile(id:string){
    this.router.navigate(['/app/patients', id]);
  }
}
