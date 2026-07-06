import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { CognitoService } from '../services/cognito-service';

export const orgGuard: CanActivateFn = () => {
  const cognitoService = inject(CognitoService);
  if (cognitoService.hasOrganization()) {
    return true;
  }
  return inject(Router).createUrlTree(['/app/create-organization']);
};
