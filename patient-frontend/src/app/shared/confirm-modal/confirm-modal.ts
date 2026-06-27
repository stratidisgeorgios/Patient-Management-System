import { Component, EventEmitter, Input, Output } from "@angular/core";

@Component({
  selector: "app-confirm-modal",
  imports: [],
  templateUrl: "./confirm-modal.html",
  styleUrl: "./confirm-modal.css",
})
export class ConfirmModal {
  @Input() message: string = "Are you sure you want to proceed?";
  @Output() confirm = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();
}
