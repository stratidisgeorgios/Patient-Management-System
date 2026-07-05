import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { CognitoService } from '../services/cognito-service';

export const authGuard: CanActivateFn = () => {
  const cognitoService = inject(CognitoService);
  if (cognitoService.authenticated()) {
    return true;
  }
  return inject(Router).createUrlTree(['/']);
};
