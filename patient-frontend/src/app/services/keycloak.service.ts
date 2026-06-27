import { Injectable, signal } from '@angular/core';
import Keycloak from 'keycloak-js';

@Injectable({ providedIn: 'root' })
export class KeycloakService {
  private keycloak = new Keycloak({
    url: 'http://localhost:8080',
    realm: 'patientmanagement',
    clientId: 'patient-frontend'
  });

  authenticated = signal(false);

  async init(): Promise<void> {
    try {
      const result = await this.keycloak.init({
        onLoad: 'check-sso',
        silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
        checkLoginIframe: false
      });
      this.authenticated.set(result);
    } catch {
      this.authenticated.set(false);
    }
  }

  login(): Promise<void> {
    return this.keycloak.login({ redirectUri: window.location.origin + '/app/patients' });
  }

  logout(): Promise<void> {
    return this.keycloak.logout({ redirectUri: window.location.origin });
  }

  async getValidToken(): Promise<string | undefined> {
    try {
      await this.keycloak.updateToken(30);
    } catch {
      await this.login();
    }
    return this.keycloak.token;
  }
}
