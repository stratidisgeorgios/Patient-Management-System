import { ComponentFixture, TestBed } from "@angular/core/testing";
import { provideRouter } from "@angular/router";
import { provideHttpClient } from "@angular/common/http";
import { APP_SERVICE_CONFIG } from "../../app-config.interface";
import { TreatmentList } from "./treatment-list";

describe("TreatmentList", () => {
  let component: TreatmentList;
  let fixture: ComponentFixture<TreatmentList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TreatmentList],
      providers: [
        provideRouter([]),
        { provide: APP_SERVICE_CONFIG, useValue: { apiUrl: "" } },
        provideHttpClient(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TreatmentList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });
});
