import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { KeycloakService } from '../services/keycloak.service';

export const authGuard: CanActivateFn = () => {
  const keycloakService = inject(KeycloakService);
  if (keycloakService.authenticated()) {
    return true;
  }
  return inject(Router).createUrlTree(['/']);
};
