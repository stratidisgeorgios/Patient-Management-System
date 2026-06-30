import { ComponentFixture, TestBed } from "@angular/core/testing";

import { TreatmentProfile } from "./treatment-profile";

describe("TreatmentProfile", () => {
  let component: TreatmentProfile;
  let fixture: ComponentFixture<TreatmentProfile>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TreatmentProfile],
    }).compileComponents();

    fixture = TestBed.createComponent(TreatmentProfile);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });
});
