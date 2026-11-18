-- ============================================================================
-- V2 — Datos iniciales de catálogo (Sprints 1-2)
-- PMS de reservas hoteleras
--
-- Datos mínimos para operar el núcleo: una propiedad, dos tipos de habitación,
-- cuatro habitaciones, tres políticas de cancelación, dos tarifas con precios
-- diarios para 2026, tres canales, tres huéspedes y los roles/usuarios base.
--
-- Los UUID son fijos y coinciden con el fixture del modelo
-- (pms-model/src/test/resources/fixtures/reservas-fixture.xmi), de modo que la
-- matriz de trazabilidad pueda referirse a las mismas entidades en ambos mundos.
--
-- NO se insertan reservas: las pruebas de concurrencia crean las suyas y así el
-- seed es idempotente respecto de cualquier ejecución de test.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- Propiedad (RN-02, RN-11, RN-14)
-- ---------------------------------------------------------------------------
INSERT INTO propiedad (
    id, nombre, moneda, zona_horaria,
    hora_check_in, hora_check_out, hora_limite_sin_garantia, hora_limite_llegada,
    overbooking_habilitado, limite_overbooking, fecha_operativa
) VALUES (
    '00000000-0000-0000-0000-000000000001', 'Hotel Andino', 'USD', 'America/Guayaquil',
    '15:00', '12:00', '18:00', '18:00',
    false, 0, DATE '2026-01-05'
);

-- ---------------------------------------------------------------------------
-- Roles y usuarios (RN-09, RN-11, RN-12, RN-14)
-- ---------------------------------------------------------------------------
INSERT INTO rol (id, nombre, permisos) VALUES
    ('00000000-0000-0000-0000-000000000701', 'GERENTE', ARRAY[
        'RESERVA_CREAR','RESERVA_MODIFICAR','RESERVA_CANCELAR',
        'DESCUENTO_APLICAR','DESCUENTO_APROBAR',
        'CHECKIN_REALIZAR','CHECKOUT_REALIZAR','FOLIO_MODIFICAR',
        'HABITACION_ESTADO_CAMBIAR','TARIFA_MODIFICAR',
        'CIERRE_EJECUTAR','EXCEPCION_CIERRE',
        'USUARIO_ADMINISTRAR','REPORTE_VER','OVERBOOKING_HABILITAR'
    ]::text[]),
    ('00000000-0000-0000-0000-000000000702', 'RECEPCION', ARRAY[
        'RESERVA_CREAR','RESERVA_MODIFICAR','RESERVA_CANCELAR',
        'CHECKIN_REALIZAR','CHECKOUT_REALIZAR','REPORTE_VER'
    ]::text[]);

INSERT INTO usuario (id, rol_id, nombre_usuario, correo, activo) VALUES
    ('00000000-0000-0000-0000-000000000801',
     '00000000-0000-0000-0000-000000000701', 'gerente', 'gerente@hotel.com', true),
    ('00000000-0000-0000-0000-000000000802',
     '00000000-0000-0000-0000-000000000702', 'recepcion', 'recepcion@hotel.com', true);

-- ---------------------------------------------------------------------------
-- Inventario: tipos y habitaciones (RN-13)
-- ---------------------------------------------------------------------------
INSERT INTO tipo_habitacion (
    id, propiedad_id, codigo, nombre, capacidad_adultos, capacidad_ninos, descripcion, activo
) VALUES
    ('00000000-0000-0000-0000-000000000101',
     '00000000-0000-0000-0000-000000000001', 'DOBLE', 'Habitación doble',
     2, 1, 'Cama queen y baño privado', true),
    ('00000000-0000-0000-0000-000000000102',
     '00000000-0000-0000-0000-000000000001', 'SUITE', 'Suite',
     3, 2, 'Dos ambientes con sala', true);

INSERT INTO habitacion (
    id, propiedad_id, tipo_habitacion_id, numero, piso, estado_limpieza, fuera_de_servicio
) VALUES
    ('00000000-0000-0000-0000-000000000201',
     '00000000-0000-0000-0000-000000000001',
     '00000000-0000-0000-0000-000000000101', '101', 1, 'LIMPIA', false),
    ('00000000-0000-0000-0000-000000000202',
     '00000000-0000-0000-0000-000000000001',
     '00000000-0000-0000-0000-000000000101', '102', 1, 'LIMPIA', false),
    ('00000000-0000-0000-0000-000000000203',
     '00000000-0000-0000-0000-000000000001',
     '00000000-0000-0000-0000-000000000102', '201', 2, 'LIMPIA', false),
    ('00000000-0000-0000-0000-000000000204',
     '00000000-0000-0000-0000-000000000001',
     '00000000-0000-0000-0000-000000000102', '202', 2, 'INSPECCIONADA', false);

-- ---------------------------------------------------------------------------
-- Políticas de cancelación (RN-05)
-- ---------------------------------------------------------------------------
INSERT INTO politica_cancelacion (
    id, codigo, horas_limite, cargo_noches, cargo_porcentaje, reembolsable
) VALUES
    ('00000000-0000-0000-0000-000000000301', 'FLEXIBLE', 48, 0, 0, true),
    ('00000000-0000-0000-0000-000000000302', 'MODERADA', 48, 1, 0, true),
    ('00000000-0000-0000-0000-000000000303', 'NO_REEMBOLSABLE', 0, 0, 100, false);

-- ---------------------------------------------------------------------------
-- Tarifas (RN-04) y precios diarios de 2026
-- ---------------------------------------------------------------------------
INSERT INTO tarifa (
    id, tipo_habitacion_id, politica_cancelacion_id, codigo, nombre, plan,
    vigencia_inicio, vigencia_fin, estancia_minima, estancia_maxima,
    cierre_llegada_dias, activa
) VALUES
    ('00000000-0000-0000-0000-000000000401',
     '00000000-0000-0000-0000-000000000101',
     '00000000-0000-0000-0000-000000000302',
     'TAR-DOBLE-2026', 'Doble 2026', 'SOLO_HABITACION',
     DATE '2026-01-01', DATE '2026-12-31', 1, 0, 0, true),
    ('00000000-0000-0000-0000-000000000402',
     '00000000-0000-0000-0000-000000000102',
     '00000000-0000-0000-0000-000000000301',
     'TAR-SUITE-2026', 'Suite 2026', 'DESAYUNO',
     DATE '2026-01-01', DATE '2026-12-31', 1, 0, 0, true);

-- Un precio por noche y tarifa para todo 2026. El UUID se deriva del contenido
-- para que la migración sea reproducible sin depender de gen_random_uuid().
INSERT INTO precio_noche (id, tarifa_id, fecha, monto, impuesto_porcentaje)
SELECT md5('precio-doble-' || dia::text)::uuid,
       '00000000-0000-0000-0000-000000000401',
       dia::date, 100.00, 16.00
FROM generate_series(DATE '2026-01-01', DATE '2026-12-31', INTERVAL '1 day') AS dia;

INSERT INTO precio_noche (id, tarifa_id, fecha, monto, impuesto_porcentaje)
SELECT md5('precio-suite-' || dia::text)::uuid,
       '00000000-0000-0000-0000-000000000402',
       dia::date, 150.00, 16.00
FROM generate_series(DATE '2026-01-01', DATE '2026-12-31', INTERVAL '1 day') AS dia;

-- ---------------------------------------------------------------------------
-- Canales (HU-30)
-- ---------------------------------------------------------------------------
INSERT INTO canal (id, nombre, tipo, comision_porcentaje) VALUES
    ('00000000-0000-0000-0000-000000000501', 'Directo', 'DIRECTO', 0),
    ('00000000-0000-0000-0000-000000000502', 'Sitio web', 'WEB', 15),
    ('00000000-0000-0000-0000-000000000503', 'OTA', 'OTA', 18);

-- ---------------------------------------------------------------------------
-- Huéspedes de ejemplo (RN-03, RN-15)
-- ---------------------------------------------------------------------------
INSERT INTO huesped (
    id, nombre, correo, telefono, documento, tipo_documento, notas,
    consentimiento_datos, fecha_consentimiento, datos_personales,
    solicitud_supresion, datos_anonimizados
) VALUES
    ('00000000-0000-0000-0000-000000000601', 'Ana Torres',
     'ana@example.com', '+593900000001', '1712345678', 'DNI', NULL,
     true, DATE '2025-06-01', true, false, false),
    ('00000000-0000-0000-0000-000000000602', 'Luis Pérez',
     'luis@example.com', '+593900000002', '1712345679', 'DNI', NULL,
     true, DATE '2025-06-01', true, false, false),
    ('00000000-0000-0000-0000-000000000603', 'Marta Ruiz',
     'marta@example.com', '+593900000003', '1712345680', 'DNI', NULL,
     true, DATE '2025-06-01', true, false, false);
