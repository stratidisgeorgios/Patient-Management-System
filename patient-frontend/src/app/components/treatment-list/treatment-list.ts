import { Component, OnDestroy, OnInit, signal } from "@angular/core";
import { TreatmentResponse, CategoryResponse } from "../../models/treatment.model";
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
import { TreatmentService } from "../../services/treatment-service";
import { NotificationService } from "../../services/notification-service";
import { ConfirmModal } from "../../shared/confirm-modal/confirm-modal";
import { CurrencyPipe } from "@angular/common";
import { CategoryService } from "../../services/category-service";
import { SearchService } from "../../services/search-service";
import { Router } from "@angular/router";
import { debounceTime, forkJoin, of, Subject, switchMap, takeUntil } from "rxjs";
import { SseService } from "../../services/sse-service";
@Component({
  selector: "app-treatment-list",
  imports: [ReactiveFormsModule, CurrencyPipe, ConfirmModal],
  templateUrl: "./treatment-list.html",
  styleUrl: "./treatment-list.css",
})
export class TreatmentList implements OnInit, OnDestroy {
  treatments = signal<TreatmentResponse[]>([]);
  treatmentToEdit = signal<TreatmentResponse | null>(null);
  treatmentToDelete = signal<TreatmentResponse | null>(null);
  categories = signal<CategoryResponse[]>([]);
  categoryToEdit = signal<CategoryResponse | null>(null);
  categoryToDelete = signal<CategoryResponse | null>(null);

  editTreatmentForm: FormGroup = new FormGroup({
    name: new FormControl('', [Validators.required]),
    category: new FormControl('', [Validators.required]),
    price: new FormControl('', [Validators.required, Validators.min(0)])
  });
  createTreatmentForm: FormGroup = new FormGroup({
    name: new FormControl('', [Validators.required]),
    category: new FormControl('', [Validators.required]),
    price: new FormControl('', [Validators.required, Validators.min(0)])
  });

  editCategoryForm: FormGroup = new FormGroup({
    name: new FormControl('', [Validators.required]),
    description: new FormControl('', [Validators.required])
  });
  createCategoryForm: FormGroup = new FormGroup({
    name: new FormControl('', [Validators.required]),
    description: new FormControl('', [Validators.required])
  });
  showTreatmentCreateModal = signal(false);
  showTreatmentEditModal = signal(false);
  showTreatmentDeleteModal = signal(false);
  showCategoriesModal = signal(false);
  showCategoryCreateModal = signal(false);
  showCategoryEditModal = signal(false);
  showCategoryDeleteModal = signal(false);
  searchQuery = new Subject<string>();
  hasSearched = signal(false);
  isLoading = signal(false);
  lastSearchQuery = '';
  private destroy$ = new Subject<void>();
  constructor(private treatmentService: TreatmentService, private categoryService: CategoryService, private searchService: SearchService, private notificationService: NotificationService, private router: Router, private sseService: SseService) {}

  ngOnInit() {
    this.categoryService.getCategories().subscribe({
      next: (data: any) => {
        this.categories.set(data.sort((a: CategoryResponse, b: CategoryResponse) => a.name.localeCompare(b.name)));
      },
      error: (err) => {
        this.notificationService.error("Failed to load categories: " + this.extractError(err));
      }
    });
    this.sseService.connect().pipe(takeUntil(this.destroy$)).subscribe({
      next: (message) => {
        if (message === 'treatment' && this.lastSearchQuery) {
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
          this.treatments.set([]);
          this.isLoading.set(false);
          return of([]);
        }
        this.hasSearched.set(true);
        this.isLoading.set(true);
        this.lastSearchQuery = value;
          return this.searchService.searchTreatments(value).pipe(
            switchMap(data => {
              if (data.length === 0) {
                return of([]);
              }
              return forkJoin(data.map(p => this.treatmentService.getById(p.id)));
            })
          );
        }),
        takeUntil(this.destroy$)
      ).subscribe({
        next: (fullTreatments) => {
          this.treatments.set(fullTreatments.sort((a, b) => a.name.localeCompare(b.name)));
          this.isLoading.set(false);
        },
        error: (err) => {
          this.notificationService.error("Failed to search treatments: " + this.extractError(err));
          this.isLoading.set(false);
        }
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private closeAllModals() {
    this.showTreatmentCreateModal.set(false);
    this.showTreatmentEditModal.set(false);
    this.showTreatmentDeleteModal.set(false);
    this.showCategoryCreateModal.set(false);
    this.showCategoryEditModal.set(false);
    this.showCategoryDeleteModal.set(false);
    this.treatmentToEdit.set(null);
    this.treatmentToDelete.set(null);
    this.categoryToEdit.set(null);
    this.categoryToDelete.set(null);
  }

  createTreatment() {
    this.closeAllModals();
    this.createTreatmentForm.reset();
    this.treatmentToEdit.set(null);
    this.showTreatmentCreateModal.set(true);
  }

  submitTreatmentCreate() {
    this.treatmentService.createTreatment(this.createTreatmentForm.value).subscribe({
      next: () => {
        this.showTreatmentCreateModal.set(false);
        this.notificationService.success("Treatment created successfully!");
      },
      error: (err) => this.notificationService.error("Failed to create treatment: " + this.extractError(err))
    });
  }

  editTreatment(treatment: TreatmentResponse) {
    this.closeAllModals();
    this.editTreatmentForm.reset();
    this.showTreatmentEditModal.set(true);
    this.treatmentService.getById(treatment.id).subscribe({
      next: (full) => {
        this.treatmentToEdit.set(full);
        this.editTreatmentForm.patchValue({
          name: full.name,
          category: full.category.id,
          price: full.price
        });
      },
      error: (err) => this.notificationService.error("Failed to load treatment: " + this.extractError(err))
    });
  }

  submitTreatmentUpdate() {
    const treatmentId = this.treatmentToEdit()!.id;
    this.treatmentService.updateTreatment(treatmentId, this.editTreatmentForm.value).subscribe({
      next: () => {
        this.showTreatmentEditModal.set(false);
        this.notificationService.success("Treatment updated successfully!");
      },
      error: (err) => this.notificationService.error("Failed to update treatment: " + this.extractError(err))
    });
  }

  deleteTreatment(treatment: TreatmentResponse) {
    this.closeAllModals();
    this.treatmentToDelete.set(treatment);
    this.showTreatmentDeleteModal.set(true);
  }

  confirmTreatmentDelete() {
    const treatmentId = this.treatmentToDelete()!.id;
    this.treatmentService.deleteTreatment(treatmentId).subscribe({
      next: () => {
        this.showTreatmentDeleteModal.set(false);
        this.notificationService.success("Treatment deleted successfully!");
      },
      error: (err) => this.notificationService.error("Failed to delete treatment: " + this.extractError(err))
    });
  }

  cancelTreatmentDelete() {
    this.treatmentToDelete.set(null);
    this.showTreatmentDeleteModal.set(false);
  }
  showCategories() {
    this.closeAllModals();
    this.showCategoriesModal.set(true);
  }

  createCategory() {
    this.closeAllModals();
    this.categoryToEdit.set(null);
    this.showCategoryCreateModal.set(true);
    this.createCategoryForm.reset();
  }

  submitCategoryCreate() {
    this.categoryService.createCategory(this.createCategoryForm.value).subscribe({
      next: () => {
        this.showCategoryCreateModal.set(false);
        this.notificationService.success("Category created successfully!");
      },
      error: (err) => this.notificationService.error("Failed to create category: " + this.extractError(err))
    });
  }

  editCategory(category: CategoryResponse) {
    this.closeAllModals();
    this.editCategoryForm.reset();
    this.categoryToEdit.set(category);
    this.showCategoryEditModal.set(true);
    this.editCategoryForm.patchValue({
      name: category.name
    });
    this.editCategoryForm.patchValue({
      description: category.description
    });
  }

  submitCategoryUpdate() {
    const categoryId = this.categoryToEdit()!.id;
    this.categoryService.updateCategory(categoryId, this.editCategoryForm.value).subscribe({
      next: () => {
        this.showCategoryEditModal.set(false);
        this.notificationService.success("Category updated successfully!");
      },
      error: (err) => this.notificationService.error("Failed to update category: " + this.extractError(err))
    });
  }

  deleteCategory(category: CategoryResponse) {
    this.closeAllModals();
    this.categoryToDelete.set(category);
    this.showCategoryDeleteModal.set(true);
  }

  confirmCategoryDelete() {
    const categoryId = this.categoryToDelete()!.id;
    this.categoryService.deleteCategory(categoryId).subscribe({
      next: () => {
        this.showCategoryDeleteModal.set(false);
        this.notificationService.success("Category deleted successfully!");
      },
      error: (err) => this.notificationService.error("Failed to delete category: " + this.extractError(err))
    });
  }

  cancelCategoryDelete() {
    this.categoryToDelete.set(null);
    this.showCategoryDeleteModal.set(false);
  }

  private extractError(err: any): string {
    if (err.error?.message) return err.error.message;
    if (typeof err.error === 'string') return err.error;
    if (err.error && typeof err.error === 'object') return Object.values(err.error).join(', ');
    return err.message || 'An unexpected error occurred';
  }
    openProfile(id:string){
    this.router.navigate(['/app/treatments', id]);
  }
}
