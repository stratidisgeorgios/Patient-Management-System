import { TestBed } from "@angular/core/testing";
import { provideHttpClient } from "@angular/common/http";
import { APP_SERVICE_CONFIG } from "../app-config.interface";
import { BillingService } from "./billing-service";

describe("BillingService", () => {
  let service: BillingService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: APP_SERVICE_CONFIG, useValue: { apiUrl: "" } },
        provideHttpClient(),
      ],
    });
    service = TestBed.inject(BillingService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });
});
