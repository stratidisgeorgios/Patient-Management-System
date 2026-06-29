import { Component, computed, signal } from "@angular/core";
import { TreatmentResponse, CategoryResponse } from "../../models/treatment.model";
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
import { TreatmentService } from "../../services/treatment-service";
import { NotificationService } from "../../services/notification-service";
import { ConfirmModal } from "../../shared/confirm-modal/confirm-modal";
import { CurrencyPipe } from "@angular/common";
import { CategoryService } from "../../services/category-service";

const PAGE_SIZE = 30;

@Component({
  selector: "app-treatment-list",
  imports: [ReactiveFormsModule, CurrencyPipe, ConfirmModal],
  templateUrl: "./treatment-list.html",
  styleUrl: "./treatment-list.css",
})
export class TreatmentList {
  treatments = signal<TreatmentResponse[]>([]);
  treatmentToEdit = signal<TreatmentResponse | null>(null);
  treatmentToDelete = signal<TreatmentResponse | null>(null);
  currentPage = signal(0);
  categories = signal<CategoryResponse[]>([]);

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
  showCreateModal = signal(false);
  showEditModal = signal(false);
  showDeleteModal = signal(false);
  showCategoriesModal = signal(false);

  constructor(private treatmentService: TreatmentService, private categoryService: CategoryService, private notificationService: NotificationService) {}

  ngOnInit() {
    this.treatmentService.getTreatments().subscribe((data: any) => {
      this.treatments.set(data.sort((a: TreatmentResponse, b: TreatmentResponse) => a.name.localeCompare(b.name)));
    });
    this.categoryService.getCategories().subscribe((data: any) => {
      this.categories.set(data.sort((a: CategoryResponse, b: CategoryResponse) => a.name.localeCompare(b.name)));
    });
  }

  createTreatment() {
    this.treatmentToEdit.set(null);
    this.showCreateModal.set(true);
    this.createTreatmentForm.reset();
  }

  submitCreate() {
    this.treatmentService.createTreatment(this.createTreatmentForm.value).subscribe(() => {
      this.treatmentService.getTreatments().subscribe((data: any) => {
        this.treatments.set(data.sort((a: TreatmentResponse, b: TreatmentResponse) => a.name.localeCompare(b.name)));
        this.showCreateModal.set(false);
        this.notificationService.success("Treatment created successfully!");
      });
    });
  }

  editTreatment(treatment: TreatmentResponse) {
    this.treatmentToEdit.set(treatment);
    this.showEditModal.set(true);
    this.editTreatmentForm.patchValue({
      name: treatment.name,
      category: treatment.category.name,
      price: treatment.price
    });
  }

  submitUpdate() {
    const treatmentId = this.treatmentToEdit()!.id;
    this.treatmentService.updateTreatment(treatmentId, this.editTreatmentForm.value).subscribe(() => {
      this.treatmentService.getTreatments().subscribe((data: any) => {
        this.treatments.set(data.sort((a: TreatmentResponse, b: TreatmentResponse) => a.name.localeCompare(b.name)));
        this.showEditModal.set(false);
        this.notificationService.success("Treatment updated successfully!");
      });
    });
  }

  deleteTreatment(treatment: TreatmentResponse) {
    this.treatmentToDelete.set(treatment);
    this.showDeleteModal.set(true);
  }

  confirmDelete() {
    const treatmentId = this.treatmentToDelete()!.id;
    this.treatmentService.deleteTreatment(treatmentId).subscribe(() => {
      this.treatmentService.getTreatments().subscribe((data: any) => {
        this.treatments.set(data.sort((a: TreatmentResponse, b: TreatmentResponse) => a.name.localeCompare(b.name)));
        this.showDeleteModal.set(false);
        this.notificationService.success("Treatment deleted successfully!");
        if (this.currentPage() >= this.totalPages()) this.currentPage.set(this.totalPages() - 1);
      });
    });
  }

  cancelDelete() {
    this.treatmentToDelete.set(null);
    this.showDeleteModal.set(false);
  }
  showCategories() {
    this.showCategoriesModal.set(true);
  }

  createCategory() {
  }

  editCategory() {
  }
  
  deleteCategory() {
  }

  prevPage() { if (this.currentPage() > 0) this.currentPage.update(p => p - 1); }
  nextPage() { if (this.currentPage() < this.totalPages() - 1) this.currentPage.update(p => p + 1); }
}


