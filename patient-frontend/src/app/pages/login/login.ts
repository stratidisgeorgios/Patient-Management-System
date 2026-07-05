import { Component } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Router } from "@angular/router";
import { CognitoService } from "../../services/cognito-service";

type Mode = 'signin' | 'signup' | 'confirm';

@Component({
  selector: "app-login",
  standalone: true,
  imports: [FormsModule],
  templateUrl: "./login.html",
  styleUrl: "./login.css",
})
export class Login {
  mode: Mode = 'signin';
  email = '';
  password = '';
  confirmationCode = '';
  error = '';
  loading = false;

  constructor(private cognitoService: CognitoService, private router: Router) {}

  async signIn(): Promise<void> {
    this.error = '';
    this.loading = true;
    try {
      await this.cognitoService.signIn(this.email, this.password);
      this.router.navigate(['/app/patients']);
    } catch (e: any) {
      this.error = e.message ?? 'Sign in failed';
    } finally {
      this.loading = false;
    }
  }

  async signUp(): Promise<void> {
    this.error = '';
    this.loading = true;
    try {
      await this.cognitoService.signUp(this.email, this.password);
      this.mode = 'confirm';
    } catch (e: any) {
      console.log('SignUp error:', e);
      this.error = e.message ?? 'Sign up failed';
    } finally {
      this.loading = false;
    }
  }

  async confirmSignUp(): Promise<void> {
    this.error = '';
    this.loading = true;
    try {
      await this.cognitoService.confirmSignUp(this.email, this.confirmationCode);
      this.mode = 'signin';
    } catch (e: any) {
      this.error = e.message ?? 'Confirmation failed';
    } finally {
      this.loading = false;
    }
  }

  switchMode(mode: Mode): void {
    this.mode = mode;
    this.error = '';
  }
}
