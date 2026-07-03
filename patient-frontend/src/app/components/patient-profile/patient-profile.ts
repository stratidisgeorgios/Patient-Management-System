import { Component, ElementRef, HostListener, OnDestroy, OnInit, signal, ViewChild } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { CurrencyPipe, Location, DatePipe } from "@angular/common";
import { PatientService } from "../../services/patient-service";
import { PatientResponse } from "../../models/patient.model";
import { BillingService } from "../../services/billing-service";
import { BillingResponse } from "../../models/billing.model";
import { ReactiveFormsModule } from "@angular/forms";
import { Subject, of } from "rxjs";
import { SearchService } from "../../services/search-service";
import { NotificationService } from "../../services/notification-service";
import { debounceTime, switchMap, takeUntil } from "rxjs/operators";
import { TreatmentSearchResponse } from "../../models/search.model";

@Component({
  selector: "app-patient-profile",
  imports: [ReactiveFormsModule,CurrencyPipe,DatePipe],
  templateUrl: "./patient-profile.html",
  styleUrl: "./patient-profile.css",
})

export class PatientProfile implements OnInit, OnDestroy {

  constructor(private route: ActivatedRoute, private location: Location, private patientService: PatientService, private billingService: BillingService, private searchService: SearchService, private notificationService: NotificationService) {}
  patient = signal<PatientResponse | null>(null);
  error: string | null = null;
  billingInfo = signal<BillingResponse | null>(null);
  searchQuery = new Subject<string>();
  treatments = signal<TreatmentSearchResponse[]>([]);
  private destroy$ = new Subject<void>();
  @ViewChild('searchContainer') searchContainer!: ElementRef;

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

  chargePatient(treatmentId: string): void {
    this.treatments.set([]);
    const patientId = this.patient()!.id;
    const treatment = this.treatments().find(t => t.id === treatmentId);
    if (!treatment) {
      this.notificationService.error('Treatment not found.');
      return;
    }
    const chargeRequest = { treatmentId: treatment.id};
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

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
