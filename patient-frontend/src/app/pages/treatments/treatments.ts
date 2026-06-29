import { Component } from "@angular/core";
import { TreatmentList } from "../../components/treatment-list/treatment-list";

@Component({
  selector: "app-treatments",
  imports: [TreatmentList],
  templateUrl: "./treatments.html",
  styleUrl: "./treatments.css",
})
export class Treatments {}
