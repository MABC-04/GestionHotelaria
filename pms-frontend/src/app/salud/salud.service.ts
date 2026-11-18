import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, timeout } from 'rxjs';

/** Respuesta de `GET /health` del backend. */
export interface Salud {
  estado: string;
  aplicacion: string;
  version: string;
  momento: string;
  baseDatos: {
    estado: string;
    migraciones?: number;
    detalle?: string;
  };
}

@Injectable({ providedIn: 'root' })
export class SaludService {
  private readonly http = inject(HttpClient);

  consultar(): Observable<Salud> {
    return this.http.get<Salud>('/health').pipe(timeout(5000));
  }
}
