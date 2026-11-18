import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { SaludComponent } from './salud.component';

describe('SaludComponent', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SaludComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('muestra el estado operativo y las migraciones', async () => {
    const fixture = TestBed.createComponent(SaludComponent);
    fixture.detectChanges();

    http.expectOne('/health').flush({
      estado: 'UP',
      aplicacion: 'pms-backend',
      version: '0.1.0-SNAPSHOT',
      momento: '2026-11-17T21:00:00-06:00',
      baseDatos: { estado: 'UP', migraciones: 2 },
    });

    await fixture.whenStable();
    fixture.detectChanges();

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texto).toContain('Operativo');
    expect(texto).toContain('pms-backend');
    expect(texto).toContain('2 migraciones aplicadas');
    expect(texto).toContain('rh_sin_solape');
  });

  it('muestra el estado degradado si la base de datos falla', async () => {
    const fixture = TestBed.createComponent(SaludComponent);
    fixture.detectChanges();

    http.expectOne('/health').flush({
      estado: 'DEGRADADO',
      aplicacion: 'pms-backend',
      version: '0.1.0-SNAPSHOT',
      momento: '2026-11-17T21:00:00-06:00',
      baseDatos: { estado: 'DOWN', detalle: 'Connection refused' },
    });

    await fixture.whenStable();
    fixture.detectChanges();

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texto).toContain('Degradado');
    expect(texto).toContain('Connection refused');
  });

  it('muestra "Sin conexión" si la API no responde', async () => {
    const fixture = TestBed.createComponent(SaludComponent);
    fixture.detectChanges();

    http
      .expectOne('/health')
      .flush({}, { status: 503, statusText: 'Service Unavailable' });

    await fixture.whenStable();
    fixture.detectChanges();

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texto).toContain('Sin conexión');
    expect(texto).toContain('spring-boot:run');
  });
});
