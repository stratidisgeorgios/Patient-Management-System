import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { from, switchMap } from 'rxjs';
import { CognitoService } from '../services/cognito-service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const cognitoService = inject(CognitoService);
  return from(cognitoService.getValidToken()).pipe(
    switchMap(token => {
      if (token) {
        req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
      }
      return next(req);
    })
  );
};
