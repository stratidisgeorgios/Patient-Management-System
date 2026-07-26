import { Inject, Injectable, OnDestroy } from "@angular/core";
import { Observable } from "rxjs";
import { EventSourcePolyfill } from "event-source-polyfill";
import { APP_SERVICE_CONFIG, AppConfig } from "../app-config.interface";
import { CognitoService } from "./cognito-service";
@Injectable({
  providedIn: "root",
})
export class SseService implements OnDestroy {
  private eventSource: EventSourcePolyfill | null = null;
  private closed = false;

  constructor(private cognitoService: CognitoService, @Inject(APP_SERVICE_CONFIG) private config: AppConfig) {}

  connect(): Observable<string> {
    return new Observable<string>(observer => {
      this.closed = false;
      const open = () => {
        if (this.closed) return;
        this.cognitoService.getValidToken().then(token => {
          if (this.closed) return;
          if (this.eventSource) this.eventSource.close();
          this.eventSource = new EventSourcePolyfill(`${this.config.apiUrl}/api/search/events`, {
            headers: { Authorization: `Bearer ${token}` }
          });
          this.eventSource.addEventListener('indexUpdated', (event: any) => {
            observer.next(event.data);
          });
          this.eventSource.onerror = () => {
            this.eventSource?.close();
            setTimeout(open, 5000);
          };
        });
      };
      open();
      return () => {
        this.closed = true;
        this.eventSource?.close();
      };
    });
  }

  ngOnDestroy(): void {
    this.closed = true;
    this.eventSource?.close();
  }
}
