import { Injectable, OnDestroy } from "@angular/core";
import { Observable } from "rxjs";
import { KeycloakService } from './keycloak.service';
import { EventSourcePolyfill } from "event-source-polyfill";
@Injectable({
  providedIn: "root",
})
export class SseService implements OnDestroy {
  private eventSource: EventSourcePolyfill | null = null;

  constructor(private keycloakService: KeycloakService) {}
  connect(): Observable<string> {
    return new Observable<string>(observer => {
      this.keycloakService.getValidToken().then(token => {
        this.eventSource = new EventSourcePolyfill('/api/search/events', {
          headers: { Authorization: `Bearer ${token}` }
        });
        this.eventSource.addEventListener('indexUpdated',(event:any) => {
          observer.next(event.data);
        });
        this.eventSource.onerror = (error: any) => {
          observer.error(error);
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
