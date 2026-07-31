import { ComponentFixture, TestBed } from "@angular/core/testing";
import { provideRouter } from "@angular/router";
import { provideHttpClient } from "@angular/common/http";
import { APP_SERVICE_CONFIG } from "../../app-config.interface";
import { Patients } from "./patients";

describe("Patients", () => {
  let component: Patients;
  let fixture: ComponentFixture<Patients>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Patients],
      providers: [
        provideRouter([]),
        { provide: APP_SERVICE_CONFIG, useValue: { apiUrl: "" } },
        provideHttpClient(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Patients);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });
});
