-- ============================================================================
-- V1 — Núcleo de inventario y reservas (Sprints 0-2)
-- PMS de reservas hoteleras
--
-- Este esquema materializa el PSM relacional del modelo reservas.ecore para
-- los paquetes habilitados en los sprints 0-2. Las tablas de estancia, folio,
-- cargos, pagos, bloqueos y grupos llegan en S3/S7.
--
-- La restricción rh_sin_solape es la implementación real de RN-01: el OCL
-- valida el modelo, PostgreSQL impide la carrera por la última habitación.
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ---------------------------------------------------------------------------
-- Propiedad y cierre del día (RN-11, RN-14)
-- ---------------------------------------------------------------------------
CREATE TABLE propiedad (
    id                        uuid PRIMARY KEY,
    nombre                    varchar(120) NOT NULL,
    moneda                    char(3)      NOT NULL DEFAULT 'USD',
    zona_horaria              varchar(64)  NOT NULL DEFAULT 'America/Guayaquil',
    hora_check_in             time         NOT NULL DEFAULT '15:00',
    hora_check_out            time         NOT NULL DEFAULT '12:00',
    hora_limite_sin_garantia  time         NOT NULL DEFAULT '18:00',
    hora_limite_llegada       time         NOT NULL DEFAULT '18:00',
    overbooking_habilitado    boolean      NOT NULL DEFAULT false,
    limite_overbooking        integer      NOT NULL DEFAULT 0,
    fecha_operativa           date         NOT NULL,
    -- RN-02: horas estándar 15:00 / 12:00
    CONSTRAINT propiedad_horas_estandar CHECK (
        date_part('hour', hora_check_in) = 15 AND date_part('hour', hora_check_out) = 12
    ),
    -- RN-14: sobreventa solo con límite explícito
    CONSTRAINT propiedad_overbooking CHECK (
        (overbooking_habilitado AND limite_overbooking > 0)
        OR (NOT overbooking_habilitado AND limite_overbooking = 0)
    )
);

CREATE TABLE cierre_dia (
    id               uuid PRIMARY KEY,
    propiedad_id     uuid NOT NULL REFERENCES propiedad(id),
    fecha_operativa  date NOT NULL,
    ejecutado_en     timestamptz NOT NULL DEFAULT now(),
    ejecutado_por    uuid NOT NULL,
    UNIQUE (propiedad_id, fecha_operativa)
);

-- ---------------------------------------------------------------------------
-- Seguridad y auditoría (RN-12, HU-26)
-- ---------------------------------------------------------------------------
CREATE TABLE rol (
    id       uuid PRIMARY KEY,
    nombre   varchar(60) NOT NULL UNIQUE,
    permisos text[] NOT NULL DEFAULT '{}',
    CONSTRAINT rol_permisos_validos CHECK (permisos <@ ARRAY[
        'RESERVA_CREAR','RESERVA_MODIFICAR','RESERVA_CANCELAR',
        'DESCUENTO_APLICAR','DESCUENTO_APROBAR',
        'CHECKIN_REALIZAR','CHECKOUT_REALIZAR','FOLIO_MODIFICAR',
        'HABITACION_ESTADO_CAMBIAR','TARIFA_MODIFICAR',
        'CIERRE_EJECUTAR','EXCEPCION_CIERRE',
        'USUARIO_ADMINISTRAR','REPORTE_VER','OVERBOOKING_HABILITAR'
    ]::text[])
);

CREATE TABLE usuario (
    id              uuid PRIMARY KEY,
    rol_id          uuid NOT NULL REFERENCES rol(id),
    nombre_usuario  varchar(60) NOT NULL UNIQUE,
    correo          varchar(160) NOT NULL,
    activo          boolean NOT NULL DEFAULT true
);

ALTER TABLE cierre_dia
    ADD CONSTRAINT cierre_dia_ejecutado_por_fk FOREIGN KEY (ejecutado_por) REFERENCES usuario(id);

CREATE TABLE bitacora (
    id             uuid PRIMARY KEY,
    usuario_id     uuid NOT NULL REFERENCES usuario(id),
    entidad        varchar(60) NOT NULL,
    entidad_id     uuid NOT NULL,
    campo          varchar(60),
    valor_anterior text,
    valor_nuevo    text,
    accion         varchar(20) NOT NULL,
    fecha          timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT bitacora_cambio_real CHECK (
        valor_anterior IS DISTINCT FROM valor_nuevo
    )
);

CREATE INDEX bitacora_entidad_idx ON bitacora (entidad, entidad_id, fecha DESC);

-- RN-12: la bitácora es inmutable a nivel de base de datos.
CREATE OR REPLACE FUNCTION bitacora_append_only() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'La bitácora es append-only (RN-12)';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_bitacora_inmutable
    BEFORE UPDATE OR DELETE ON bitacora
    FOR EACH ROW EXECUTE FUNCTION bitacora_append_only();

-- ---------------------------------------------------------------------------
-- Inventario: tipos de habitación y habitaciones (RN-13)
-- ---------------------------------------------------------------------------
CREATE TABLE tipo_habitacion (
    id                 uuid PRIMARY KEY,
    propiedad_id       uuid NOT NULL REFERENCES propiedad(id),
    codigo             varchar(20) NOT NULL,
    nombre             varchar(80) NOT NULL,
    capacidad_adultos  integer NOT NULL CHECK (capacidad_adultos > 0),
    capacidad_ninos    integer NOT NULL DEFAULT 0 CHECK (capacidad_ninos >= 0),
    descripcion        text,
    activo             boolean NOT NULL DEFAULT true,
    UNIQUE (propiedad_id, codigo)
);

CREATE TABLE habitacion (
    id                   uuid PRIMARY KEY,
    propiedad_id         uuid NOT NULL REFERENCES propiedad(id),
    tipo_habitacion_id   uuid NOT NULL REFERENCES tipo_habitacion(id),
    numero               varchar(10) NOT NULL,
    piso                 integer NOT NULL,
    estado_limpieza      varchar(20) NOT NULL DEFAULT 'LIMPIA',
    fuera_de_servicio    boolean NOT NULL DEFAULT false,
    CONSTRAINT habitacion_limpieza CHECK (estado_limpieza IN (
        'OCUPADA','SUCIA','EN_LIMPIEZA','LIMPIA','INSPECCIONADA','FUERA_DE_SERVICIO'
    )),
    UNIQUE (propiedad_id, numero)
);

CREATE INDEX habitacion_tipo_idx ON habitacion (tipo_habitacion_id)
    WHERE NOT fuera_de_servicio;

-- ---------------------------------------------------------------------------
-- Tarifas (RN-04, RN-05) y canales (HU-30)
-- ---------------------------------------------------------------------------
CREATE TABLE politica_cancelacion (
    id                uuid PRIMARY KEY,
    codigo            varchar(30) NOT NULL UNIQUE,
    horas_limite      integer NOT NULL DEFAULT 0 CHECK (horas_limite >= 0),
    cargo_noches      integer NOT NULL DEFAULT 0 CHECK (cargo_noches >= 0),
    cargo_porcentaje  numeric(5,2) NOT NULL DEFAULT 0 CHECK (cargo_porcentaje BETWEEN 0 AND 100),
    reembolsable      boolean NOT NULL,
    CONSTRAINT politica_flexible CHECK (
        codigo <> 'FLEXIBLE' OR (horas_limite = 48 AND cargo_noches = 0 AND reembolsable)
    ),
    CONSTRAINT politica_moderada CHECK (
        codigo <> 'MODERADA' OR (cargo_noches = 1 AND reembolsable)
    ),
    CONSTRAINT politica_no_reembolsable CHECK (
        codigo <> 'NO_REEMBOLSABLE' OR (cargo_porcentaje = 100 AND NOT reembolsable)
    )
);

CREATE TABLE tarifa (
    id                      uuid PRIMARY KEY,
    tipo_habitacion_id      uuid NOT NULL REFERENCES tipo_habitacion(id),
    politica_cancelacion_id uuid NOT NULL REFERENCES politica_cancelacion(id),
    codigo                  varchar(30) NOT NULL UNIQUE,
    nombre                  varchar(80) NOT NULL,
    plan                    varchar(20) NOT NULL,
    vigencia_inicio         date NOT NULL,
    vigencia_fin            date NOT NULL,
    estancia_minima         integer NOT NULL DEFAULT 1 CHECK (estancia_minima >= 1),
    estancia_maxima         integer NOT NULL DEFAULT 0 CHECK (estancia_maxima >= 0),
    cierre_llegada_dias     integer NOT NULL DEFAULT 0 CHECK (cierre_llegada_dias >= 0),
    activa                  boolean NOT NULL DEFAULT true,
    CONSTRAINT tarifa_plan CHECK (plan IN ('SOLO_HABITACION','DESAYUNO','MEDIA_PENSION')),
    CONSTRAINT tarifa_vigencia CHECK (vigencia_inicio <= vigencia_fin),
    CONSTRAINT tarifa_estancia CHECK (estancia_maxima = 0 OR estancia_maxima >= estancia_minima)
);

CREATE TABLE precio_noche (
    id                  uuid PRIMARY KEY,
    tarifa_id           uuid NOT NULL REFERENCES tarifa(id) ON DELETE CASCADE,
    fecha               date NOT NULL,
    monto               numeric(12,2) NOT NULL CHECK (monto >= 0),
    impuesto_porcentaje numeric(5,2) NOT NULL DEFAULT 0 CHECK (impuesto_porcentaje BETWEEN 0 AND 100),
    UNIQUE (tarifa_id, fecha)
);

CREATE TABLE canal (
    id                   uuid PRIMARY KEY,
    nombre               varchar(60) NOT NULL UNIQUE,
    tipo                 varchar(20) NOT NULL,
    comision_porcentaje  numeric(5,2) NOT NULL DEFAULT 0 CHECK (comision_porcentaje BETWEEN 0 AND 100),
    CONSTRAINT canal_tipo CHECK (tipo IN (
        'DIRECTO','TELEFONO','WALK_IN','WEB','OTA','AGENCIA','CORPORATIVO'
    ))
);

-- ---------------------------------------------------------------------------
-- Huéspedes (RN-03, RN-15)
-- ---------------------------------------------------------------------------
CREATE TABLE huesped (
    id                      uuid PRIMARY KEY,
    nombre                  varchar(120) NOT NULL,
    correo                  varchar(160),
    telefono                varchar(40),
    documento               varchar(40),
    tipo_documento          varchar(20),
    notas                   text,
    consentimiento_datos    boolean NOT NULL DEFAULT false,
    fecha_consentimiento    date,
    datos_personales        boolean NOT NULL DEFAULT false,
    solicitud_supresion     boolean NOT NULL DEFAULT false,
    datos_anonimizados      boolean NOT NULL DEFAULT false,
    CONSTRAINT huesped_tipo_documento CHECK (
        tipo_documento IS NULL OR tipo_documento IN ('DNI','PASAPORTE','NIT','OTRO')
    ),
    CONSTRAINT huesped_contacto CHECK (correo IS NOT NULL OR telefono IS NOT NULL),
    CONSTRAINT huesped_correo CHECK (
        correo IS NULL OR correo ~ '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'
    ),
    -- RN-15: tratar datos personales exige consentimiento fechado
    CONSTRAINT huesped_consentimiento CHECK (
        NOT datos_personales OR (consentimiento_datos AND fecha_consentimiento IS NOT NULL)
    ),
    CONSTRAINT huesped_supresion CHECK (
        NOT solicitud_supresion OR (datos_anonimizados AND notas IS NULL)
    )
);

-- ---------------------------------------------------------------------------
-- Reservas (RN-01, RN-03, RN-06, RN-08, RN-09)
-- ---------------------------------------------------------------------------
CREATE TABLE reserva (
    id                       uuid PRIMARY KEY,
    propiedad_id             uuid NOT NULL REFERENCES propiedad(id),
    titular_id               uuid NOT NULL REFERENCES huesped(id),
    canal_id                 uuid NOT NULL REFERENCES canal(id),
    politica_cancelacion_id  uuid NOT NULL REFERENCES politica_cancelacion(id),
    codigo_confirmacion      varchar(8) NOT NULL UNIQUE,
    fecha_entrada            date NOT NULL,
    fecha_salida             date NOT NULL,
    adultos                  integer NOT NULL CHECK (adultos >= 1),
    ninos                    integer NOT NULL DEFAULT 0 CHECK (ninos >= 0),
    estado                   varchar(12) NOT NULL DEFAULT 'PENDIENTE',
    estado_activo            boolean GENERATED ALWAYS AS (
                                 estado IN ('PENDIENTE','CONFIRMADA','EN_CASA')
                             ) STORED,
    descuento_porcentaje     numeric(5,2) NOT NULL DEFAULT 0
                                 CHECK (descuento_porcentaje BETWEEN 0 AND 100),
    total                    numeric(12,2) NOT NULL DEFAULT 0 CHECK (total >= 0),
    hora_limite_llegada      time,
    creada_en                timestamptz NOT NULL DEFAULT now(),
    modificada_en            timestamptz,
    modificada_por           uuid REFERENCES usuario(id),
    fecha_no_show            timestamptz,
    fecha_cancelacion        timestamptz,
    motivo_cancelacion       text,
    cargo_cancelacion        numeric(12,2) NOT NULL DEFAULT 0 CHECK (cargo_cancelacion >= 0),
    cargo_no_show            numeric(12,2) NOT NULL DEFAULT 0 CHECK (cargo_no_show >= 0),
    CONSTRAINT reserva_estado CHECK (estado IN (
        'PENDIENTE','CONFIRMADA','EN_CASA','FINALIZADA','CANCELADA','NO_SHOW','EXPIRADA'
    )),
    CONSTRAINT reserva_fechas CHECK (fecha_entrada < fecha_salida)
);

CREATE INDEX reserva_estado_idx ON reserva (estado, fecha_entrada, fecha_salida);
CREATE INDEX reserva_titular_idx ON reserva (titular_id);

CREATE TABLE reserva_habitacion (
    id                  uuid PRIMARY KEY,
    reserva_id          uuid NOT NULL REFERENCES reserva(id) ON DELETE CASCADE,
    habitacion_id       uuid REFERENCES habitacion(id),
    tipo_habitacion_id  uuid NOT NULL REFERENCES tipo_habitacion(id),
    tarifa_id           uuid NOT NULL REFERENCES tarifa(id),
    fecha_entrada       date NOT NULL,
    fecha_salida        date NOT NULL,
    noches              integer GENERATED ALWAYS AS (fecha_salida - fecha_entrada) STORED,
    precio_total        numeric(12,2) NOT NULL CHECK (precio_total >= 0),
    -- Espejo de reserva.estado_activo, mantenido por trigger: es el predicado
    -- parcial que PostgreSQL exige para una restricción de exclusión.
    estado_activo       boolean NOT NULL DEFAULT true,
    CONSTRAINT linea_fechas CHECK (fecha_entrada < fecha_salida)
);

CREATE INDEX reserva_habitacion_rango_idx
    ON reserva_habitacion (habitacion_id, fecha_entrada, fecha_salida);

-- RN-01 — la habitación no puede tener dos reservas activas solapadas.
-- La noche de salida no se solapa con la de entrada: rango '[)'.
ALTER TABLE reserva_habitacion
    ADD CONSTRAINT rh_sin_solape
    EXCLUDE USING gist (
        habitacion_id WITH =,
        daterange(fecha_entrada, fecha_salida, '[)') WITH &&
    ) WHERE (estado_activo AND habitacion_id IS NOT NULL);

CREATE OR REPLACE FUNCTION heredar_estado_activo() RETURNS trigger AS $$
BEGIN
    SELECT estado IN ('PENDIENTE','CONFIRMADA','EN_CASA') INTO NEW.estado_activo
      FROM reserva WHERE id = NEW.reserva_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_linea_estado_activo
    BEFORE INSERT OR UPDATE OF reserva_id, estado_activo ON reserva_habitacion
    FOR EACH ROW EXECUTE FUNCTION heredar_estado_activo();

CREATE OR REPLACE FUNCTION sincronizar_estado_lineas() RETURNS trigger AS $$
BEGIN
    IF OLD.estado IS DISTINCT FROM NEW.estado THEN
        UPDATE reserva_habitacion
           SET estado_activo = NEW.estado IN ('PENDIENTE','CONFIRMADA','EN_CASA')
         WHERE reserva_id = NEW.id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_reserva_estado_lineas
    AFTER UPDATE OF estado ON reserva
    FOR EACH ROW EXECUTE FUNCTION sincronizar_estado_lineas();

-- ---------------------------------------------------------------------------
-- Garantías (RN-06), retenciones (CU-02) y aprobación de descuentos (RN-09)
-- ---------------------------------------------------------------------------
CREATE TABLE garantia (
    id                     uuid PRIMARY KEY,
    reserva_id             uuid NOT NULL UNIQUE REFERENCES reserva(id) ON DELETE CASCADE,
    tipo                   varchar(20) NOT NULL,
    monto                  numeric(12,2) NOT NULL DEFAULT 0 CHECK (monto >= 0),
    referencia             varchar(120),
    autorizada_en          timestamptz,
    hora_limite_liberacion timestamptz,
    CONSTRAINT garantia_tipo CHECK (tipo IN ('TARJETA','DEPOSITO','EMPRESA','SIN_GARANTIA')),
    CONSTRAINT garantia_autorizacion CHECK (
        tipo = 'SIN_GARANTIA' OR autorizada_en IS NOT NULL
    ),
    CONSTRAINT garantia_sin_garantia CHECK (
        tipo <> 'SIN_GARANTIA' OR hora_limite_liberacion IS NOT NULL
    )
);

CREATE TABLE retencion_inventario (
    id                  uuid PRIMARY KEY,
    tipo_habitacion_id  uuid NOT NULL REFERENCES tipo_habitacion(id),
    reserva_id          uuid REFERENCES reserva(id) ON DELETE SET NULL,
    cantidad            integer NOT NULL DEFAULT 1 CHECK (cantidad > 0),
    fecha_entrada       date NOT NULL,
    fecha_salida        date NOT NULL,
    creada_en           timestamptz NOT NULL DEFAULT now(),
    expira_en           timestamptz NOT NULL,
    consumida_en        timestamptz,
    token_sesion        varchar(120) NOT NULL,
    estado              varchar(12) NOT NULL DEFAULT 'ACTIVA',
    CONSTRAINT retencion_estado CHECK (estado IN ('ACTIVA','CONSUMIDA','EXPIRADA','LIBERADA')),
    CONSTRAINT retencion_fechas CHECK (fecha_entrada < fecha_salida),
    -- CU-02: TTL exacto de 10 minutos
    CONSTRAINT retencion_ttl CHECK (expira_en = creada_en + interval '10 minutes'),
    CONSTRAINT retencion_expirada CHECK (estado <> 'EXPIRADA' OR consumida_en IS NULL)
);

CREATE INDEX retencion_vigente_idx ON retencion_inventario (expira_en) WHERE estado = 'ACTIVA';

CREATE TABLE aprobacion_descuento (
    id                   uuid PRIMARY KEY,
    reserva_id           uuid NOT NULL UNIQUE REFERENCES reserva(id) ON DELETE CASCADE,
    aprobado_por         uuid NOT NULL REFERENCES usuario(id),
    porcentaje_aprobado  numeric(5,2) NOT NULL CHECK (porcentaje_aprobado BETWEEN 0 AND 100),
    fecha_aprobacion     timestamptz NOT NULL DEFAULT now(),
    justificacion        text NOT NULL
);
