import { Component } from "@angular/core";
import { CatalogList } from "../../components/catalog-list/catalog-list";

@Component({
  selector: "app-catalog",
  imports: [CatalogList],
  templateUrl: "./catalog.html",
  styleUrl: "./catalog.css",
})
export class Catalog {}
