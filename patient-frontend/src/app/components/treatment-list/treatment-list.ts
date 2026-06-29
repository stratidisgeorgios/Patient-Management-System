import { Component, computed, OnInit, signal } from "@angular/core";
import { TreatmentResponse, CategoryResponse } from "../../models/treatment.model";
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
import { TreatmentService } from "../../services/treatment-service";
import { NotificationService } from "../../services/notification-service";
import { ConfirmModal } from "../../shared/confirm-modal/confirm-modal";
import { CurrencyPipe } from "@angular/common";
import { CategoryService } from "../../services/category-service";
import { SearchService } from "../../services/search-service";

const PAGE_SIZE = 30;

@Component({
  selector: "app-treatment-list",
  imports: [ReactiveFormsModule, CurrencyPipe, ConfirmModal],
  templateUrl: "./treatment-list.html",
  styleUrl: "./treatment-list.css",
})
export class TreatmentList implements OnInit {
  treatments = signal<TreatmentResponse[]>([]);
  treatmentToEdit = signal<TreatmentResponse | null>(null);
  treatmentToDelete = signal<TreatmentResponse | null>(null);
  currentPage = signal(0);
  categories = signal<CategoryResponse[]>([]);
  categoryToEdit = signal<CategoryResponse | null>(null);
  categoryToDelete = signal<CategoryResponse | null>(null);

  pagedTreatments = computed(() =>
    this.treatments().slice(this.currentPage() * PAGE_SIZE, (this.currentPage() + 1) * PAGE_SIZE)
  );
  totalPages = computed(() => Math.ceil(this.treatments().length / PAGE_SIZE));

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

  constructor(private treatmentService: TreatmentService, private categoryService: CategoryService, private searchService: SearchService, private notificationService: NotificationService) {}

  ngOnInit() {
    this.treatmentService.getTreatments().subscribe({
      next: (data: any) => {
        this.treatments.set(data.sort((a: TreatmentResponse, b: TreatmentResponse) => a.name.localeCompare(b.name)));
      },
      error: (err) => {
        this.notificationService.error("Failed to load treatments: " + this.extractError(err));
      }
    });
    this.categoryService.getCategories().subscribe({
      next: (data: any) => {
        this.categories.set(data.sort((a: CategoryResponse, b: CategoryResponse) => a.name.localeCompare(b.name)));
      },
      error: (err) => {
        this.notificationService.error("Failed to load categories: " + this.extractError(err));
      }
    });
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
        this.treatmentService.getTreatments().subscribe((data: any) => {
          this.treatments.set(data.sort((a: TreatmentResponse, b: TreatmentResponse) => a.name.localeCompare(b.name)));
          this.showTreatmentCreateModal.set(false);
          this.notificationService.success("Treatment created successfully!");
        });
      },
      error: (err) => this.notificationService.error("Failed to create treatment: " + this.extractError(err))
    });
  }

  editTreatment(treatment: TreatmentResponse) {
    this.closeAllModals();
    this.editTreatmentForm.reset();
    this.treatmentToEdit.set(treatment);
    this.showTreatmentEditModal.set(true);
    this.editTreatmentForm.patchValue({
      name: treatment.name,
      category: treatment.category.id,
      price: treatment.price
    });
  }

  submitTreatmentUpdate() {
    const treatmentId = this.treatmentToEdit()!.id;
    this.treatmentService.updateTreatment(treatmentId, this.editTreatmentForm.value).subscribe({
      next: () => {
        this.treatmentService.getTreatments().subscribe((data: any) => {
          this.treatments.set(data.sort((a: TreatmentResponse, b: TreatmentResponse) => a.name.localeCompare(b.name)));
          this.showTreatmentEditModal.set(false);
          this.notificationService.success("Treatment updated successfully!");
        });
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
        this.treatmentService.getTreatments().subscribe((data: any) => {
          this.treatments.set(data.sort((a: TreatmentResponse, b: TreatmentResponse) => a.name.localeCompare(b.name)));
          this.showTreatmentDeleteModal.set(false);
          this.notificationService.success("Treatment deleted successfully!");
          if (this.currentPage() >= this.totalPages()) this.currentPage.set(this.totalPages() - 1);
        });
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
        this.categoryService.getCategories().subscribe((data: any) => {
          this.categories.set(data.sort((a: CategoryResponse, b: CategoryResponse) => a.name.localeCompare(b.name)));
          this.showCategoryCreateModal.set(false);
          this.notificationService.success("Category created successfully!");
        });
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
        this.categoryService.getCategories().subscribe((data: any) => {
          this.categories.set(data.sort((a: CategoryResponse, b: CategoryResponse) => a.name.localeCompare(b.name)));
          this.showCategoryEditModal.set(false);
          this.notificationService.success("Category updated successfully!");
        });
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
        this.categoryService.getCategories().subscribe((data: any) => {
          this.categories.set(data.sort((a: CategoryResponse, b: CategoryResponse) => a.name.localeCompare(b.name)));
          this.showCategoryDeleteModal.set(false);
          this.notificationService.success("Category deleted successfully!");
        });
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

  prevPage() { if (this.currentPage() > 0) this.currentPage.update(p => p - 1); }
  nextPage() { if (this.currentPage() < this.totalPages() - 1) this.currentPage.update(p => p + 1); }

  onSearch(value: string) {
    this.searchService.searchTreatments(value).subscribe({
      next: (data: any) => {
        this.treatments.set(data.sort((a: TreatmentResponse, b: TreatmentResponse) => a.name.localeCompare(b.name)));
        this.currentPage.set(0);
      },
      error: (err) => {
        this.notificationService.error("Failed to search treatments: " + this.extractError(err));
      }
    });
    

  }

}
