import { InjectionToken } from '@angular/core';

export interface AppConfig {
  apiUrl: string;
}

export const APP_SERVICE_CONFIG = new InjectionToken<AppConfig>('app.config');
