import { TestBed } from "@angular/core/testing";
import { provideHttpClient } from "@angular/common/http";
import { APP_SERVICE_CONFIG } from "../app-config.interface";
import { TreatmentService } from "./treatment-service";

describe("TreatmentService", () => {
  let service: TreatmentService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: APP_SERVICE_CONFIG, useValue: { apiUrl: "" } },
        provideHttpClient(),
      ],
    });
    service = TestBed.inject(TreatmentService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });
});
