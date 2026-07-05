import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { CognitoService } from '../services/cognito-service';

export const guestGuard: CanActivateFn = () => {
  const cognitoService = inject(CognitoService);
  if (cognitoService.authenticated()) {
    return inject(Router).createUrlTree(['/app/admin']);
  }
  return true;
};
