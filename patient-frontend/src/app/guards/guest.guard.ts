import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { KeycloakService } from '../services/keycloak.service';

export const guestGuard: CanActivateFn = () => {
  const keycloakService = inject(KeycloakService);
  if (keycloakService.authenticated()) {
    return inject(Router).createUrlTree(['/app/admin']);
  }
  return true;
};
