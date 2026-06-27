import { Injectable, signal } from "@angular/core";

export interface Notification {
  message: string;
  type: "success" | "error" | "info";
  id: number;
}

@Injectable({providedIn:'root'})
export class NotificationService {
  notifications = signal<Notification[]>([]);
  private counter =0;

  error(message: string) {
    this.add(message, "error");
  }

  success(message: string) {
    this.add(message, "success");
  }

  info(message: string) {
    this.add(message, "info");
  }

  private add(message:string,type:Notification['type']){
    const id = this.counter++;
    this.notifications.update(n=> [...n, { message,type,id}]);
    setTimeout(() => this.remove(id), 4000);
  }

  remove(id:number){
    this.notifications.update(n => n.filter(notification => notification.id !== id));
  }


}
