import { Component } from "@angular/core";
import { NotificationService } from "../../services/notification-service";

@Component({
  selector: "app-toast",
  imports: [],
  standalone: true,
  templateUrl: "./toast.html",
  styleUrl: "./toast.css",
})
export class Toast {
  constructor(public notificationService: NotificationService) {}
}
