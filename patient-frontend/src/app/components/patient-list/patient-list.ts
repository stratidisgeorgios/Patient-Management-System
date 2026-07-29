import { Component, OnDestroy, OnInit, signal } from "@angular/core";
import { ImportJobStatus, PatientResponse } from "../../models/patient.model";
import { PatientSearchResponse } from "../../models/search.model";
import { PatientService } from "../../services/patient-service";
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';
import { ConfirmModal } from "../../shared/confirm-modal/confirm-modal";
import { NotificationService } from "../../services/notification-service";
import { SearchService } from "../../services/search-service";
import { Router } from "@angular/router";
import { debounceTime, of, Subject, switchMap, takeUntil } from "rxjs";
import { SseService } from "../../services/sse-service";

@Component({
  selector: "app-patient-list",
  imports: [ReactiveFormsModule, ConfirmModal],
  standalone: true,
  templateUrl: "./patient-list.html",
  styleUrl: "./patient-list.css",
})
export class PatientList implements OnInit, OnDestroy {
  patients = signal<PatientSearchResponse[]>([]);
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

  showImportModal = signal(false);
  importStep = signal<'upload' | 'mapping' | 'status'>('upload');
  importMapping: Record<string, string> = {};
  importS3Key = '';
  importTotalRows = 0;
  importJobId = '';
  importJobStatus = signal<ImportJobStatus | null>(null);
  isUploading = signal(false);
  uploadProgress = signal(0);
  uploadSpeed = signal('');
  uploadEta = signal('');
  private uploadStartTime = 0;
  private isPolling = false;

  readonly knownFields = [
    { value: 'name', label: 'Name' },
    { value: 'email', label: 'Email' },
    { value: 'gender', label: 'Gender' },
    { value: 'address', label: 'Address' },
    { value: 'dateOfBirth', label: 'Date of Birth' },
    { value: 'registeredDate', label: 'Registered Date' },
  ];

  constructor(
    private patientService: PatientService,
    private searchService: SearchService,
    private notificationService: NotificationService,
    private router: Router,
    private sseService: SseService
  ) {}

  ngOnInit() {
    this.sseService.connect().pipe(takeUntil(this.destroy$)).subscribe({
      next: (message) => {
        if (message === 'patient' && this.lastSearchQuery) {
          this.searchQuery.next(this.lastSearchQuery);
        }
      },
      error: (err) => console.error("SSE connection error:", err)
    });
    this.searchQuery.pipe(
      debounceTime(200),
      switchMap(value => {
        if (!value.trim() || value.trim().length < 2) {
          this.hasSearched.set(false);
          this.patients.set([]);
          this.isLoading.set(false);
          return of([]);
        }
        this.hasSearched.set(true);
        this.isLoading.set(true);
        this.lastSearchQuery = value;
        return this.searchService.searchPatients(value);
      }),
      takeUntil(this.destroy$)
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
    this.isPolling = false;
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
    this.createForm.patchValue({ registeredDate: new Date().toISOString().split('T')[0] });
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

  updatePatient(patient: PatientSearchResponse) {
    this.closeAllModals();
    this.patientService.getById(patient.id).subscribe({
      next: (fullPatient) => {
        this.selectedPatient.set(fullPatient);
        this.showEditModal.set(true);
        this.editForm.patchValue({
          name: fullPatient.name,
          email: fullPatient.email,
          gender: fullPatient.gender,
          address: fullPatient.address,
          dateOfBirth: fullPatient.dateOfBirth
        });
      },
      error: (err) => this.notificationService.error("Failed to load patient: " + this.extractError(err))
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

  deletePatient(patient: PatientSearchResponse) {
    this.closeAllModals();
    this.selectedPatient.set(patient as unknown as PatientResponse);
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

  openProfile(id: string) {
    this.router.navigate(['/app/patients', id]);
  }

  openImport() {
    this.showImportModal.set(true);
    this.importStep.set('upload');
    this.importS3Key = '';
    this.importMapping = {};
    this.importTotalRows = 0;
    this.importJobId = '';
    this.importJobStatus.set(null);
    this.isUploading.set(false);
    this.isPolling = false;
  }

  closeImportModal() {
    this.isPolling = false;
    this.showImportModal.set(false);
  }

  onFileSelected(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.isUploading.set(true);
    this.uploadProgress.set(0);
    this.uploadSpeed.set('');
    this.uploadEta.set('');
    this.uploadStartTime = Date.now();

    const onProgress = (loaded: number, total: number) => {
      const pct = Math.round((loaded / total) * 100);
      const elapsed = (Date.now() - this.uploadStartTime) / 1000;
      const speedBps = loaded / elapsed;
      const remaining = (total - loaded) / speedBps;
      this.uploadProgress.set(pct);
      this.uploadSpeed.set(this.formatSpeed(speedBps));
      this.uploadEta.set(remaining > 0 ? this.formatTime(remaining) : '');
    };

    const onUploadDone = (s3Key: string) => {
      this.importS3Key = s3Key;
      this.isUploading.set(false);
      this.importStep.set('mapping');
    };

    const onUploadError = (msg: string) => {
      this.isUploading.set(false);
      this.notificationService.error(msg);
    };

    this.readCsvInfo(file).then(({ headers, estimatedRows }) => {
      this.importMapping = this.mapHeaders(headers);
      this.importTotalRows = estimatedRows;

      if (file.size >= 50 * 1024 * 1024) {
        this.patientService.uploadLargeFileToS3(file, onProgress)
          .then((s3Key) => onUploadDone(s3Key))
          .catch((err) => onUploadError("Failed to upload file: " + err.message));
      } else {
        this.patientService.getPresignedUrl().subscribe({
          next: ({ presignedUrl, s3Key }) => {
            this.patientService.uploadToS3(presignedUrl, file, onProgress)
              .then(() => onUploadDone(s3Key))
              .catch((err) => onUploadError("Failed to upload file: " + err.message));
          },
          error: (err) => onUploadError("Failed to get upload URL: " + this.extractError(err))
        });
      }
    }).catch((err) => {
      this.isUploading.set(false);
      this.notificationService.error("Failed to read file: " + err.message);
    });
  }

  private readCsvInfo(file: File): Promise<{ headers: string[], estimatedRows: number }> {
    return new Promise((resolve, reject) => {
      const blob = file.slice(0, 64 * 1024);
      const reader = new FileReader();
      reader.onload = (e) => {
        const text = e.target?.result as string;
        const lines = text.split('\n').filter(l => l.trim());
        if (!lines.length) { reject(new Error('Empty file')); return; }
        const headers = lines[0].split(',').map(h => h.trim().replace(/^"|"$/g, ''));
        const avgLineBytes = text.length / lines.length;
        const estimatedRows = Math.max(0, Math.round(file.size / avgLineBytes) - 1);
        resolve({ headers, estimatedRows });
      };
      reader.onerror = () => reject(new Error('Failed to read file'));
      reader.readAsText(blob);
    });
  }

  private formatSpeed(bps: number): string {
    if (bps >= 1024 * 1024) return `${(bps / 1024 / 1024).toFixed(1)} MB/s`;
    if (bps >= 1024) return `${(bps / 1024).toFixed(1)} KB/s`;
    return `${bps.toFixed(0)} B/s`;
  }

  private formatTime(seconds: number): string {
    if (seconds >= 3600) return `${Math.floor(seconds / 3600)}h ${Math.floor((seconds % 3600) / 60)}m left`;
    if (seconds >= 60) return `${Math.floor(seconds / 60)}m ${Math.floor(seconds % 60)}s left`;
    return `${Math.floor(seconds)}s left`;
  }

  private mapHeaders(headers: string[]): Record<string, string> {
    const aliases: Record<string, string[]> = {
      name: ['name', 'fullname', 'patientname', 'nombre', 'firstname', 'lastname', 'surname'],
      email: ['email', 'emailaddress', 'mail', 'correo'],
      dateOfBirth: ['dateofbirth', 'dob', 'birthday', 'birthdate'],
      gender: ['gender', 'sex'],
      address: ['address', 'addr', 'direccion'],
      registeredDate: ['registereddate', 'joindate', 'registrationdate'],
    };
    const mapping: Record<string, string> = {};
    for (const header of headers) {
      const normalized = header.toLowerCase().replace(/[^a-z0-9]/g, '');
      const found = Object.entries(aliases).find(([, list]) => list.includes(normalized));
      mapping[header] = found ? found[0] : header;
    }
    return mapping;
  }

  updateMapping(csvColumn: string, event: Event) {
    const field = (event.target as HTMLSelectElement).value;
    this.importMapping = { ...this.importMapping, [csvColumn]: field };
  }

  isKnownField(field: string): boolean {
    return this.knownFields.some(f => f.value === field);
  }

  mappingEntries(): { csvColumn: string; field: string }[] {
    return Object.entries(this.importMapping).map(([csvColumn, field]) => ({ csvColumn, field }));
  }

  confirmMapping() {
    this.patientService.startImport(this.importS3Key, this.importMapping, this.importTotalRows).subscribe({
      next: (response) => {
        this.importJobId = response.importJobId;
        this.importStep.set('status');
        this.startPolling();
      },
      error: (err) => this.notificationService.error("Failed to start import: " + this.extractError(err))
    });
  }

  private startPolling() {
    this.isPolling = true;
    const poll = () => {
      if (!this.isPolling) return;
      this.patientService.getImportJobStatus(this.importJobId).subscribe({
        next: (status) => {
          this.importJobStatus.set(status);
          if (status.status !== 'COMPLETED' && status.status !== 'FAILED') {
            setTimeout(poll, 2000);
          } else {
            this.isPolling = false;
          }
        },
        error: (err) => {
          this.notificationService.error("Failed to get import status: " + this.extractError(err));
          this.isPolling = false;
        }
      });
    };
    poll();
  }

  getProgressPercent(): number {
    const status = this.importJobStatus();
    if (!status || status.totalRows === 0) return 0;
    return Math.min(100, Math.round((status.importedRows / status.totalRows) * 100));
  }

  private extractError(err: any): string {
    if (err.error?.message) return err.error.message;
    if (typeof err.error === 'string') return err.error;
    if (err.error && typeof err.error === 'object') return Object.values(err.error).join(', ');
    return err.message || 'An unexpected error occurred';
  }
}
