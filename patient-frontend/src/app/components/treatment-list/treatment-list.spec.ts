import { ComponentFixture, TestBed } from "@angular/core/testing";

import { TreatmentList } from "./treatment-list";

describe("TreatmentList", () => {
  let component: TreatmentList;
  let fixture: ComponentFixture<TreatmentList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TreatmentList],
    }).compileComponents();

    fixture = TestBed.createComponent(TreatmentList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });
});
