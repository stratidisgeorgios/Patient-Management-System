import { Component, ElementRef, HostListener, OnDestroy, OnInit, signal, ViewChild } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { CurrencyPipe, Location, DatePipe } from "@angular/common";
import { PatientService } from "../../services/patient-service";
import { PatientResponse } from "../../models/patient.model";
import { BillingService } from "../../services/billing-service";
import { BillingResponse } from "../../models/billing.model";
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
import { Subject, of } from "rxjs";
import { SearchService } from "../../services/search-service";
import { NotificationService } from "../../services/notification-service";
import { debounceTime, switchMap, takeUntil } from "rxjs/operators";
import { TreatmentSearchResponse } from "../../models/search.model";
import { ConfirmModal } from "../../shared/confirm-modal/confirm-modal";

@Component({
  selector: "app-patient-profile",
  imports: [ReactiveFormsModule, CurrencyPipe, DatePipe, ConfirmModal],
  templateUrl: "./patient-profile.html",
  styleUrl: "./patient-profile.css",
})
export class PatientProfile implements OnInit, OnDestroy {
  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private location: Location,
    private patientService: PatientService,
    private billingService: BillingService,
    private searchService: SearchService,
    private notificationService: NotificationService
  ) {}

  patient = signal<PatientResponse | null>(null);
  error: string | null = null;
  billingInfo = signal<BillingResponse | null>(null);
  searchQuery = new Subject<string>();
  treatments = signal<TreatmentSearchResponse[]>([]);
  showEditModal = signal(false);
  showDeleteModal = signal(false);
  private destroy$ = new Subject<void>();

  @ViewChild('searchContainer') searchContainer!: ElementRef;

  editForm = new FormGroup({
    name: new FormControl('', [Validators.required, Validators.maxLength(100)]),
    email: new FormControl('', [Validators.required, Validators.email]),
    gender: new FormControl('', [Validators.required]),
    address: new FormControl('', [Validators.required, Validators.maxLength(200)]),
    dateOfBirth: new FormControl('', [Validators.required])
  });

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (this.searchContainer && !this.searchContainer.nativeElement.contains(event.target)) {
      this.treatments.set([]);
    }
  }

  goBack(): void {
    this.location.back();
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.patientService.getById(id).subscribe({
        next: patient => { this.patient.set(patient); },
        error: () => { this.error = 'Failed to load patient. Please go back and try again.'; }
      });
      this.billingService.getBillingInfo(id).subscribe({
        next: billingInfo => { this.billingInfo.set(billingInfo); },
        error: () => { this.error = 'Failed to load billing information. Please go back and try again.'; }
      });
    }

    this.searchQuery.pipe(
      debounceTime(200),
      switchMap(value => {
        if (!value.trim()) {
          this.treatments.set([]);
          return of([]);
        }
        return this.searchService.searchTreatments(value);
      }),
      takeUntil(this.destroy$)
    ).subscribe({
      next: (results) => {
        this.treatments.set(results.sort((a, b) => a.name.localeCompare(b.name)));
      },
      error: (err) => {
        this.notificationService.error('Failed to search treatments: ' + err.message);
      }
    });
  }

  openEdit(): void {
    const p = this.patient();
    if (!p) return;
    this.editForm.patchValue({
      name: p.name,
      email: p.email,
      gender: p.gender,
      address: p.address,
      dateOfBirth: p.dateOfBirth
    });
    this.showEditModal.set(true);
  }

  submitUpdate(): void {
    const id = this.patient()!.id;
    this.patientService.update(id, this.editForm.value as any).subscribe({
      next: (updated) => {
        this.patient.set(updated);
        this.showEditModal.set(false);
        this.notificationService.success('Patient updated successfully');
      },
      error: (err) => this.notificationService.error('Failed to update patient: ' + this.extractError(err))
    });
  }

  openDelete(): void {
    this.showDeleteModal.set(true);
  }

  submitDelete(): void {
    const id = this.patient()!.id;
    this.patientService.delete(id).subscribe({
      next: () => {
        this.notificationService.success('Patient deleted successfully');
        this.router.navigate(['/app/patients']);
      },
      error: (err) => this.notificationService.error('Failed to delete patient: ' + this.extractError(err))
    });
  }

  cancelDelete(): void {
    this.showDeleteModal.set(false);
  }

  chargePatient(treatmentId: string): void {
    this.treatments.set([]);
    const patientId = this.patient()!.id;
    const chargeRequest = { treatmentId };
    this.billingService.addCharge(patientId, chargeRequest).subscribe({
      next: () => {
        this.notificationService.success('Charge added successfully.');
        this.billingService.getBillingInfo(patientId).subscribe({
          next: billingInfo => { this.billingInfo.set(billingInfo); },
          error: () => { this.error = 'Failed to refresh billing information.'; }
        });
      },
      error: (err) => {
        this.notificationService.error('Failed to add charge: ' + err.message);
      }
    });
  }

  removeCharge(chargeId: string): void {
    const patientId = this.patient()!.id;
    this.billingService.removeCharge(patientId, chargeId).subscribe({
      next: () => {
        this.notificationService.success('Charge removed successfully.');
        this.billingService.getBillingInfo(patientId).subscribe({
          next: billingInfo => { this.billingInfo.set(billingInfo); },
          error: () => { this.error = 'Failed to refresh billing information.'; }
        });
      },
      error: (err) => {
        this.notificationService.error('Failed to remove charge: ' + err.message);
      }
    });
  }

  private extractError(err: any): string {
    if (err.error?.message) return err.error.message;
    if (typeof err.error === 'string') return err.error;
    if (err.error && typeof err.error === 'object') return Object.values(err.error).join(', ');
    return err.message || 'An unexpected error occurred';
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  downloadInvoice(): void {
    const patientId = this.patient()!.id;
    this.billingService.getInvoice(patientId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `invoice_${patientId}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        this.notificationService.error('Failed to generate invoice: ' + err.message);
      }
    });
  }
}
