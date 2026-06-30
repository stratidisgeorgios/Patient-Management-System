import { Component } from "@angular/core";
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
export class TreatmentProfile {
    constructor(private route: ActivatedRoute, private location: Location, private treatmentService: TreatmentService) {}
  treatment: TreatmentResponse | null = null;
  goBack(): void {
    this.location.back();
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.treatmentService.getById(id).subscribe(treatment => {
        this.treatment = treatment;
      });
    }
  }

}
