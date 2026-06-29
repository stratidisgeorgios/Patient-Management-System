import { Routes } from '@angular/router';
import { Home } from './pages/home/home';
import { Login } from './pages/login/login';
import { About } from './pages/about/about';
import { Patients } from './pages/patients/patients';
import { Billing } from './pages/billing/billing';
import { Treatments } from './pages/treatments/treatments';
import { Analytics } from './pages/analytics/analytics';
import { Layout } from './layout/layout';
import { HomeLayout } from './layout/home-layout/home-layout';
import { authGuard } from './guards/auth.guard';
import { guestGuard } from './guards/guest.guard';

export const routes: Routes = [
  {
    path: '',
    component: HomeLayout,
    children: [
      { path: '', component: Home },
      { path: 'login', component: Login, canActivate: [guestGuard] },
      { path: 'about', component: About },
    ]
  },
  {
    path: 'app',
    component: Layout,
    canActivate: [authGuard],
    children: [
      { path: 'patients', component: Patients },
      { path: 'billing', component: Billing },
      { path: 'treatments', component: Treatments },
      { path: 'analytics', component: Analytics }
    ]
  }
];
