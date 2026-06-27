import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { NotificationService } from '../services/notification-service';
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
    const notificationService = inject(NotificationService);
    return next(req).pipe(
        catchError((error:HttpErrorResponse) => {
            switch (error.status) {
                case 401:
                    notificationService.error('Unauthorized access. Please log in.');
                    break;
                case 403:
                    notificationService.error('Forbidden access. You do not have permission to perform this action.');
                    break;
                case 404:
                    notificationService.error('Resource not found. Please check the URL or try again later.');
                    break;
                case 500:
                    notificationService.error('Internal server error. Please try again later.');
                    break;
                default:
                    notificationService.error('An unexpected error occurred. Please try again later.');
            }
            return throwError(() => error);     
        })
    );
};
