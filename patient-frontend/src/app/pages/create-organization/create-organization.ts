import { Component, signal } from "@angular/core";
import { Router } from "@angular/router";
import { OrganizationService } from "../../services/organization-service";
import { NotificationService } from "../../services/notification-service";
import { CognitoService } from "../../services/cognito-service";
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";

@Component({
  selector: "app-create-organization",
  imports: [ReactiveFormsModule],
  templateUrl: "./create-organization.html",
  styleUrl: "./create-organization.css",
})
export class CreateOrganization {
  constructor(
    private organizationService: OrganizationService,
    private router: Router,
    private notificationService: NotificationService,
    private cognitoService: CognitoService
  ) {}

  isLoading = signal(false);

  editForm = new FormGroup({
    name: new FormControl('', [Validators.required]),
    adminEmail: new FormControl('', [Validators.required, Validators.email])
  });

  createOrganization(): void {
    if (this.editForm.invalid) return;
    this.isLoading.set(true);
    this.organizationService.create({
      name: this.editForm.value.name!,
      adminEmail: this.editForm.value.adminEmail!
    }).subscribe({
      next: async () => {
        await this.cognitoService.refreshSession();
        this.notificationService.success('Organisation created successfully!');
        this.router.navigate(['/app/patients']);
      },
      error: (e) => {
        this.notificationService.error('Failed to create organisation: ' + e.message);
        this.isLoading.set(false);
      }
    });
  }
}
