import { Component } from "@angular/core";
import { KeycloakService } from "../../services/keycloak.service";

@Component({
  selector: "app-login",
  standalone: true,
  imports: [],
  templateUrl: "./login.html",
  styleUrl: "./login.css",
})
export class Login {
  constructor(public keycloakService: KeycloakService) {}
}
