import { Component } from "@angular/core";
import { RouterOutlet } from "@angular/router";
import { Header } from "../shared/header/header";
import { Sidebar } from "../shared/sidebar/sidebar";
import { Footer } from "../shared/footer/footer";
@Component({
  selector: "app-layout",
  standalone: true,
  imports: [RouterOutlet, Header, Sidebar, Footer],
  templateUrl: "./layout.html",
  styleUrl: "./layout.css",
})
export class Layout {



}
