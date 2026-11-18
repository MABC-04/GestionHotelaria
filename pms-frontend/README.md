# PMS — Frontend

Consola Angular 22 del PMS de reservas hoteleras. En la entrega 1 muestra el
estado del sistema consumiendo `GET /health` del backend.

## Desarrollo

```powershell
npm.cmd install
npm.cmd start
```

- Aplicación: `http://localhost:4200`
- API: `http://localhost:8080` (proxy definido en `proxy.conf.json`)
- Pruebas: `npm.cmd test -- --watch=false`
- Build: `npm.cmd run build`

La pantalla distingue tres estados: **Operativo** (`estado: UP` y base de datos
viva), **Degradado** (la API responde pero la base falla) y **Sin conexión**
(no hay backend). También muestra el número de migraciones aplicadas por
Flyway y recuerda el hito M1 (`rh_sin_solape`).

## Estructura

```
src/app/app.*            marco visual (cabecera, contenido, pie)
src/app/salud/           servicio, componente y pruebas de la pantalla de salud
proxy.conf.json          proxy /health y /actuator hacia el backend
```

El arranque es standalone (`src/main.ts` + `app.config.ts`), con `provideHttpClient`
y router. Las pruebas usan Vitest a través del builder `@angular/build:unit-test`.
