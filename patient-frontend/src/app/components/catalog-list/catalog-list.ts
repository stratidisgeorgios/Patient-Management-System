import { Component, computed, signal } from "@angular/core";
import { TreatmentResponse } from "../../models/catalog.model";
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
import { CatalogService } from "../../services/catalog-service";
import { NotificationService } from "../../services/notification-service";
import { ConfirmModal } from "../../shared/confirm-modal/confirm-modal";
import { CurrencyPipe } from "@angular/common";

const PAGE_SIZE = 30;

@Component({
  selector: "app-catalog-list",
  imports: [ReactiveFormsModule, CurrencyPipe, ConfirmModal],
  templateUrl: "./catalog-list.html",
  styleUrl: "./catalog-list.css",
})
export class CatalogList {
  treatments = signal<TreatmentResponse[]>([]);
  treatmentToEdit = signal<TreatmentResponse | null>(null);
  treatmentToDelete = signal<TreatmentResponse | null>(null);
  currentPage = signal(0);

  pagedTreatments = computed(() =>
    this.treatments().slice(this.currentPage() * PAGE_SIZE, (this.currentPage() + 1) * PAGE_SIZE)
  );
  totalPages = computed(() => Math.ceil(this.treatments().length / PAGE_SIZE));

  editForm: FormGroup = new FormGroup({
    name: new FormControl('', [Validators.required]),
    category: new FormControl('', [Validators.required]),
    price: new FormControl('', [Validators.required, Validators.min(0)])
  });
  createForm: FormGroup = new FormGroup({
    name: new FormControl('', [Validators.required]),
    category: new FormControl('', [Validators.required]),
    price: new FormControl('', [Validators.required, Validators.min(0)])
  });
  showCreateModal = signal(false);
  showEditModal = signal(false);
  showDeleteModal = signal(false);

  constructor(private catalogService: CatalogService, private notificationService: NotificationService) {}

  ngOnInit() {
    this.catalogService.getCatalog().subscribe((data: any) => {
      this.treatments.set(data.sort((a: TreatmentResponse, b: TreatmentResponse) => a.name.localeCompare(b.name)));
    });
  }

  createTreatment() {
    this.treatmentToEdit.set(null);
    this.showCreateModal.set(true);
    this.createForm.reset();
  }

  submitCreate() {
    this.catalogService.createTreatment(this.createForm.value).subscribe(() => {
      this.catalogService.getCatalog().subscribe((data: any) => {
        this.treatments.set(data.sort((a: TreatmentResponse, b: TreatmentResponse) => a.name.localeCompare(b.name)));
        this.showCreateModal.set(false);
        this.notificationService.success("Treatment created successfully!");
      });
    });
  }

  editTreatment(treatment: TreatmentResponse) {
    this.treatmentToEdit.set(treatment);
    this.showEditModal.set(true);
    this.editForm.patchValue({
      name: treatment.name,
      category: treatment.category,
      price: treatment.price
    });
  }

  submitUpdate() {
    const treatmentId = this.treatmentToEdit()!.id;
    this.catalogService.updateTreatment(treatmentId, this.editForm.value).subscribe(() => {
      this.catalogService.getCatalog().subscribe((data: any) => {
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
    this.catalogService.deleteTreatment(treatmentId).subscribe(() => {
      this.catalogService.getCatalog().subscribe((data: any) => {
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

  prevPage() { if (this.currentPage() > 0) this.currentPage.update(p => p - 1); }
  nextPage() { if (this.currentPage() < this.totalPages() - 1) this.currentPage.update(p => p + 1); }
}
