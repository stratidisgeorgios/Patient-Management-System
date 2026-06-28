import {Inject, Injectable, signal} from "@angular/core";
import {APP_SERVICE_CONFIG, AppConfig} from "../app-config.interface";
import {HttpClient} from "@angular/common/http";
import {tap} from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  isLoggedIn = signal(!!localStorage.getItem('token'));
  constructor(@Inject(APP_SERVICE_CONFIG) private config: AppConfig, private http: HttpClient) {}
  login(email: string, password: string) {
    return this.http.post(`${this.config.apiUrl}/api/auth/login`, { email, password }).pipe(tap(response => {
      console.log('Login response:', response);
    }));
  }
  logout(){
    localStorage.removeItem('token');
    this.isLoggedIn.set(false);
  }
}
