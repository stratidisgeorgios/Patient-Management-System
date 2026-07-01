import { Component, OnInit, signal } from "@angular/core";
import { TreatmentResponse } from "../../models/treatment.model";
import { ActivatedRoute } from "@angular/router";
import { Location } from "@angular/common";
import { TreatmentService } from "../../services/treatment-service";
import { CurrencyPipe } from "@angular/common";
@Component({
  selector: "app-treatment-profile",
  imports: [CurrencyPipe],
  templateUrl: "./treatment-profile.html",
  styleUrl: "./treatment-profile.css",
})
export class TreatmentProfile implements OnInit {
    constructor(private route: ActivatedRoute, private location: Location, private treatmentService: TreatmentService) {}
  treatment = signal<TreatmentResponse | null>(null);
  error: string | null = null;

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
  }

}
