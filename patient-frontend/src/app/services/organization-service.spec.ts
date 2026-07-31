import { TestBed } from "@angular/core/testing";
import { provideHttpClient } from "@angular/common/http";
import { APP_SERVICE_CONFIG } from "../app-config.interface";
import { OrganizationService } from "./organization-service";

describe("OrganizationService", () => {
  let service: OrganizationService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: APP_SERVICE_CONFIG, useValue: { apiUrl: "" } },
        provideHttpClient(),
      ],
    });
    service = TestBed.inject(OrganizationService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });
});
