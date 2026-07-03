import { Component, OnInit, signal } from "@angular/core";
import { CategoryResponse, TreatmentResponse } from "../../models/treatment.model";
import { ActivatedRoute, Router } from "@angular/router";
import { CurrencyPipe, Location } from "@angular/common";
import { TreatmentService } from "../../services/treatment-service";
import { CategoryService } from "../../services/category-service";
import { NotificationService } from "../../services/notification-service";
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
import { ConfirmModal } from "../../shared/confirm-modal/confirm-modal";

@Component({
  selector: "app-treatment-profile",
  imports: [CurrencyPipe, ReactiveFormsModule, ConfirmModal],
  templateUrl: "./treatment-profile.html",
  styleUrl: "./treatment-profile.css",
})
export class TreatmentProfile implements OnInit {
  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private location: Location,
    private treatmentService: TreatmentService,
    private categoryService: CategoryService,
    private notificationService: NotificationService
  ) {}

  treatment = signal<TreatmentResponse | null>(null);
  categories = signal<CategoryResponse[]>([]);
  error: string | null = null;
  showEditModal = signal(false);
  showDeleteModal = signal(false);

  editForm = new FormGroup({
    name: new FormControl('', [Validators.required]),
    category: new FormControl('', [Validators.required]),
    price: new FormControl('', [Validators.required, Validators.min(0)])
  });

  goBack(): void {
    this.location.back();
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.treatmentService.getById(id).subscribe({
        next: treatment => { this.treatment.set(treatment); },
        error: () => { this.error = 'Failed to load treatment. Please go back and try again.'; }
      });
    }
    this.categoryService.getCategories().subscribe({
      next: categories => { this.categories.set(categories); }
    });
  }

  openEdit(): void {
    const t = this.treatment();
    if (!t) return;
    this.editForm.patchValue({
      name: t.name,
      category: t.category.id,
      price: t.price.toString()
    });
    this.showEditModal.set(true);
  }

  submitUpdate(): void {
    const id = this.treatment()!.id;
    this.treatmentService.updateTreatment(id, this.editForm.value as any).subscribe({
      next: (updated) => {
        this.treatment.set(updated);
        this.showEditModal.set(false);
        this.notificationService.success('Treatment updated successfully');
      },
      error: (err) => this.notificationService.error('Failed to update treatment: ' + this.extractError(err))
    });
  }

  openDelete(): void {
    this.showDeleteModal.set(true);
  }

  submitDelete(): void {
    const id = this.treatment()!.id;
    this.treatmentService.deleteTreatment(id).subscribe({
      next: () => {
        this.notificationService.success('Treatment deleted successfully');
        this.router.navigate(['/app/treatments']);
      },
      error: (err) => this.notificationService.error('Failed to delete treatment: ' + this.extractError(err))
    });
  }

  cancelDelete(): void {
    this.showDeleteModal.set(false);
  }

  private extractError(err: any): string {
    if (err.error?.message) return err.error.message;
    if (typeof err.error === 'string') return err.error;
    if (err.error && typeof err.error === 'object') return Object.values(err.error).join(', ');
    return err.message || 'An unexpected error occurred';
  }
}
