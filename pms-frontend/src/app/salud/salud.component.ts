import { Component, OnInit, computed, inject, signal } from '@angular/core';

import { Salud, SaludService } from './salud.service';

type Tono = 'cargando' | 'ok' | 'alerta' | 'error';

@Component({
  selector: 'app-salud',
  templateUrl: './salud.component.html',
  styleUrl: './salud.component.css',
})
export class SaludComponent implements OnInit {
  private readonly servicio = inject(SaludService);

  readonly cargando = signal(true);
  readonly salud = signal<Salud | null>(null);
  readonly error = signal<string | null>(null);

  readonly tono = computed<Tono>(() => {
    if (this.cargando()) {
      return 'cargando';
    }
    if (this.error()) {
      return 'error';
    }
    return this.salud()?.estado === 'UP' ? 'ok' : 'alerta';
  });

  readonly titulo = computed(() => {
    switch (this.tono()) {
      case 'cargando':
        return 'Comprobando…';
      case 'error':
        return 'Sin conexión';
      case 'alerta':
        return 'Degradado';
      default:
        return 'Operativo';
    }
  });

  readonly momento = computed(() => {
    const valor = this.salud()?.momento;
    if (!valor) {
      return '—';
    }
    const fecha = new Date(valor);
    if (Number.isNaN(fecha.getTime())) {
      return valor;
    }
    return fecha.toLocaleString('es-EC', {
      dateStyle: 'medium',
      timeStyle: 'medium',
    });
  });

  readonly migraciones = computed(() => this.salud()?.baseDatos?.migraciones ?? null);
  readonly detalleBase = computed(() => this.salud()?.baseDatos?.detalle ?? null);

  ngOnInit(): void {
    this.consultar();
  }

  consultar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.servicio.consultar().subscribe({
      next: (salud) => {
        this.salud.set(salud);
        this.cargando.set(false);
      },
      error: (fallo: { message?: string }) => {
        this.salud.set(null);
        this.error.set(
          fallo?.message ??
            'No se pudo contactar con la API. Comprueba que el backend esté arrancado en el puerto 8080.',
        );
        this.cargando.set(false);
      },
    });
  }
}
