import { TestBed } from "@angular/core/testing";
import { provideHttpClient } from "@angular/common/http";
import { APP_SERVICE_CONFIG } from "../app-config.interface";
import { CategoryService } from "./category-service";

describe("CategoryService", () => {
  let service: CategoryService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: APP_SERVICE_CONFIG, useValue: { apiUrl: "" } },
        provideHttpClient(),
      ],
    });
    service = TestBed.inject(CategoryService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });
});
