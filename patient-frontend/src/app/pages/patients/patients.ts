import { Component } from "@angular/core";
import { PatientList } from "../../components/patient-list/patient-list";

@Component({
  selector: "app-patients",
  imports: [PatientList],
  templateUrl: "./patients.html",
  styleUrl: "./patients.css",
})
export class Patients {}
