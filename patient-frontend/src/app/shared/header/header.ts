import { Component } from "@angular/core";
import { RouterLink } from "@angular/router";
import { CognitoService } from "../../services/cognito-service";

@Component({
  selector: "app-header",
  standalone: true,
  imports: [RouterLink],
  templateUrl: "./header.html",
  styleUrl: "./header.css",
})
export class Header {
  constructor(public cognitoService: CognitoService) {}
}
