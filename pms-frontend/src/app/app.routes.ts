import { Routes } from '@angular/router';

import { SaludComponent } from './salud/salud.component';

export const routes: Routes = [
  { path: '', component: SaludComponent, title: 'Salud del sistema' },
  { path: '**', redirectTo: '' },
];
