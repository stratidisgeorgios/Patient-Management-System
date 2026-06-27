import { Component } from "@angular/core";
import { BillingService } from "../../services/billing-service";

@Component({
  selector: "app-billing",
  imports: [],
  templateUrl: "./billing.html",
  styleUrl: "./billing.css",
})
export class Billing {
  private billingService: BillingService;

  constructor(billingService: BillingService) {
    this.billingService = billingService;
  }


}
