import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: 'games/:gameId', children: [] },
  { path: '', pathMatch: 'full', children: [] },
  { path: '**', redirectTo: '' },
];
