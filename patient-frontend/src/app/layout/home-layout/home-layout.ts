import { Component } from "@angular/core";
import { RouterOutlet } from "@angular/router";
import { Header } from "../../shared/header/header";
import { Footer } from "../../shared/footer/footer";

@Component({
  selector: "app-home-layout",
  standalone: true,
  imports: [RouterOutlet, Header, Footer],
  templateUrl: "./home-layout.html",
  styleUrl: "./home-layout.css",
})
export class HomeLayout {}
