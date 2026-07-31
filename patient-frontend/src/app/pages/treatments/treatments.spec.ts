import { ComponentFixture, TestBed } from "@angular/core/testing";
import { provideRouter } from "@angular/router";
import { provideHttpClient } from "@angular/common/http";
import { APP_SERVICE_CONFIG } from "../../app-config.interface";
import { Treatments } from "./treatments";

describe("Treatments", () => {
  let component: Treatments;
  let fixture: ComponentFixture<Treatments>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Treatments],
      providers: [
        provideRouter([]),
        { provide: APP_SERVICE_CONFIG, useValue: { apiUrl: "" } },
        provideHttpClient(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Treatments);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });
});
