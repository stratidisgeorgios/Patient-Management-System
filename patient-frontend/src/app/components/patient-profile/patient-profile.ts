import { Component, OnInit, signal } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { Location } from "@angular/common";
import { PatientService } from "../../services/patient-service";
import { PatientResponse } from "../../models/patient.model";

@Component({
  selector: "app-patient-profile",
  imports: [],
  templateUrl: "./patient-profile.html",
  styleUrl: "./patient-profile.css",
})

export class PatientProfile implements OnInit {

  constructor(private route: ActivatedRoute, private location: Location, private patientService: PatientService) {}
  patient = signal<PatientResponse | null>(null);
  error: string | null = null;

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
    }
  }

}
