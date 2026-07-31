import { ComponentFixture, TestBed } from "@angular/core/testing";
import { provideRouter } from "@angular/router";
import { provideHttpClient } from "@angular/common/http";
import { APP_SERVICE_CONFIG } from "../../app-config.interface";
import { CreateOrganization } from "./create-organization";

describe("CreateOrganization", () => {
  let component: CreateOrganization;
  let fixture: ComponentFixture<CreateOrganization>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CreateOrganization],
      providers: [
        provideRouter([]),
        { provide: APP_SERVICE_CONFIG, useValue: { apiUrl: "" } },
        provideHttpClient(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CreateOrganization);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });
});
