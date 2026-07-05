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

  constructor(private cognitoService: CognitoService, @Inject(APP_SERVICE_CONFIG) private config: AppConfig) {}
  connect(): Observable<string> {
    return new Observable<string>(observer => {
      this.cognitoService.getValidToken().then(token => {
        this.eventSource = new EventSourcePolyfill(`${this.config.apiUrl}/api/search/events`, {
          headers: { Authorization: `Bearer ${token}` }
        });
        this.eventSource.addEventListener('indexUpdated', (event: any) => {
          observer.next(event.data);
        });
        this.eventSource.onerror = (error: any) => {
          console.warn('SSE reconnecting...', error);
        };
      })
    })
  }
  ngOnDestroy(): void {
    if (this.eventSource) {
      this.eventSource.close();
    }
  }
}
