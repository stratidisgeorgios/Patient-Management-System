import { TestBed } from "@angular/core/testing";
import { APP_SERVICE_CONFIG } from "../app-config.interface";
import { SseService } from "./sse-service";

describe("SseService", () => {
  let service: SseService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: APP_SERVICE_CONFIG, useValue: { apiUrl: "" } },
      ],
    });
    service = TestBed.inject(SseService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });
});
