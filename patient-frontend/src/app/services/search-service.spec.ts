import { TestBed } from "@angular/core/testing";
import { provideHttpClient } from "@angular/common/http";
import { APP_SERVICE_CONFIG } from "../app-config.interface";
import { SearchService } from "./search-service";

describe("SearchService", () => {
  let service: SearchService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: APP_SERVICE_CONFIG, useValue: { apiUrl: "" } },
        provideHttpClient(),
      ],
    });
    service = TestBed.inject(SearchService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });
});
