import { Routes } from '@angular/router';
import { MainLayoutComponent } from './shared/layout/main-layout.component';

export const routes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
        title: 'Dashboard · Interview Scheduler',
        data: { animation: 'dashboard' }
      },
      {
        path: 'upload',
        loadComponent: () =>
          import('./features/upload/upload.component').then((m) => m.UploadComponent),
        title: 'Upload Candidates · Interview Scheduler',
        data: { animation: 'upload' }
      },
      {
        path: 'batches',
        loadComponent: () =>
          import('./features/batches/batches-list.component').then((m) => m.BatchesListComponent),
        title: 'Batches · Interview Scheduler',
        data: { animation: 'batches' }
      },
      {
        path: 'batches/:id',
        loadComponent: () =>
          import('./features/batches/batch-detail.component').then((m) => m.BatchDetailComponent),
        title: 'Batch Detail · Interview Scheduler',
        data: { animation: 'batchDetail' }
      },
      {
        path: 'templates',
        loadComponent: () =>
          import('./features/templates/templates.component').then((m) => m.TemplatesComponent),
        title: 'Templates · Interview Scheduler',
        data: { animation: 'templates' }
      },
      {
        path: 'whatsapp',
        loadComponent: () =>
          import('./features/whatsapp/whatsapp-connect.component').then((m) => m.WhatsAppConnectComponent),
        title: 'WhatsApp Connection · Interview Scheduler',
        data: { animation: 'whatsapp' }
      }
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];

