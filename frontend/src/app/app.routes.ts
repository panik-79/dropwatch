import { Routes } from '@angular/router';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { FlagsAdminComponent } from './components/flags-admin/flags-admin.component';
import { SettingsComponent } from './components/settings/settings.component';

export const routes: Routes = [
  { path: '', component: DashboardComponent },
  { path: 'flags', component: FlagsAdminComponent },
  { path: 'settings', component: SettingsComponent },
  { path: '**', redirectTo: '' }
];
