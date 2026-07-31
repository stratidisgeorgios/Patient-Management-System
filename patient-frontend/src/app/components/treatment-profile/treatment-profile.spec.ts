import { ComponentFixture, TestBed } from "@angular/core/testing";
import { provideRouter } from "@angular/router";
import { provideHttpClient } from "@angular/common/http";
import { APP_SERVICE_CONFIG } from "../../app-config.interface";
import { TreatmentProfile } from "./treatment-profile";

describe("TreatmentProfile", () => {
  let component: TreatmentProfile;
  let fixture: ComponentFixture<TreatmentProfile>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TreatmentProfile],
      providers: [
        provideRouter([]),
        { provide: APP_SERVICE_CONFIG, useValue: { apiUrl: "" } },
        provideHttpClient(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TreatmentProfile);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });
});
