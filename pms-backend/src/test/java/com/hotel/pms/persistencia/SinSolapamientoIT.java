package com.hotel.pms.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.condition.EnabledIf;
import org.postgresql.ds.PGSimpleDataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Verificación real de RN-01 en PostgreSQL 16.
 *
 * <p>El invariante OCL valida modelos; la carrera por la última habitación la
 * resuelve la restricción de exclusión {@code rh_sin_solape}. Este test es el
 * gate de release del hito M1: <em>el sistema nunca vende dos veces la misma
 * habitación</em>.</p>
 *
 * <p>Se ejecuta contra la base indicada por {@code PMS_TEST_DB_URL} (por
 * defecto {@code jdbc:postgresql://localhost:5432/postgres}) y crea un esquema
 * aleatorio para no tocar datos de desarrollo. Si no hay PostgreSQL escuchando,
 * el contenedor se marca como <em>skipped</em> y el build sigue en verde; en CI
 * el servicio siempre está disponible y el test es obligatorio.</p>
 */
@DisplayName("RN-01 en PostgreSQL: rh_sin_solape")
@EnabledIf("postgresDisponible")
class SinSolapamientoIT {

    private static final String URL = entorno("PMS_TEST_DB_URL", "jdbc:postgresql://localhost:5432/postgres");
    private static final String USUARIO = entorno("PMS_DB_USER", "postgres");
    private static final String CLAVE = entorno("PMS_DB_PASSWORD", "postgres");

    private static final String PROPIEDAD = "00000000-0000-0000-0000-000000000001";
    private static final String TIPO_DOBLE = "00000000-0000-0000-0000-000000000101";
    private static final String HAB_101 = "00000000-0000-0000-0000-000000000201";
    private static final String POLITICA = "00000000-0000-0000-0000-000000000302";
    private static final String TARIFA = "00000000-0000-0000-0000-000000000401";
    private static final String CANAL = "00000000-0000-0000-0000-000000000501";
    private static final String HUESPED = "00000000-0000-0000-0000-000000000601";

    private static String esquema;
    private static boolean esquemaCreado;
    private static PGSimpleDataSource fuente;
    private static HikariDataSource pool;

    /** Habilita la clase solo si hay un PostgreSQL escuchando. */
    static boolean postgresDisponible() {
        try (Connection c = java.sql.DriverManager.getConnection(URL, USUARIO, CLAVE)) {
            return true;
        } catch (SQLException e) {
            System.err.println("Sin PostgreSQL en " + URL + ": " + e.getMessage());
            return false;
        }
    }

    @BeforeAll
    static void prepararBaseDeDatos() {
        esquema = "pms_it_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        try (Connection admin = java.sql.DriverManager.getConnection(URL, USUARIO, CLAVE);
                Statement s = admin.createStatement()) {
            s.execute("CREATE SCHEMA " + esquema);
            esquemaCreado = true;
        } catch (SQLException e) {
            Assumptions.assumeTrue(false,
                    "PostgreSQL no disponible en " + URL + " (" + e.getMessage()
                            + "). Instala PostgreSQL 16 o define PMS_TEST_DB_URL para ejecutar la prueba.");
            return;
        }

        fuente = new PGSimpleDataSource();
        fuente.setUrl(URL);
        fuente.setUser(USUARIO);
        fuente.setPassword(CLAVE);
        fuente.setCurrentSchema(esquema + ",public");

        Flyway.configure()
                .dataSource(fuente)
                .locations("classpath:db/migration")
                .schemas(esquema)
                .defaultSchema(esquema)
                .load()
                .migrate();

        HikariConfig configuracion = new HikariConfig();
        configuracion.setDataSource(fuente);
        configuracion.setPoolName("pms-it");
        // 100 tareas compiten por 24 conexiones: el cuello de botella es la
        // restricción de exclusión, no el número de conexiones del servidor.
        configuracion.setMaximumPoolSize(24);
        configuracion.setMinimumIdle(2);
        configuracion.setConnectionInitSql("SET search_path TO " + esquema + ", public");
        pool = new HikariDataSource(configuracion);
    }

    @AfterAll
    static void limpiar() {
        if (pool != null) {
            pool.close();
        }
        if (esquema == null || !esquemaCreado) {
            return;
        }
        try (Connection admin = java.sql.DriverManager.getConnection(URL, USUARIO, CLAVE);
                Statement s = admin.createStatement()) {
            s.execute("DROP SCHEMA IF EXISTS " + esquema + " CASCADE");
        } catch (SQLException e) {
            System.err.println("No se pudo limpiar el esquema " + esquema + ": " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- pruebas

    @Test
    @DisplayName("El esquema y el seed se aplican sin errores")
    void migracionesAplicadas() throws SQLException {
        try (Connection c = pool.getConnection()) {
            assertEquals(4, contar(c, "habitacion"), "habitaciones del seed");
            assertEquals(2, contar(c, "tarifa"), "tarifas del seed");
            assertEquals(730, contar(c, "precio_noche"), "precios de 2026 (2 tarifas x 365)");
            assertEquals(2, contar(c, "flyway_schema_history"), "migraciones V1 y V2");
        }
    }

    @Test
    @DisplayName("Un solapamiento en la misma habitación se rechaza con SQLState 23P01")
    void rechazaSolapamiento() throws SQLException {
        insertarLinea(nuevaReserva(codigo(), "2029-05-10", "2029-05-13"), HAB_101, "2029-05-10", "2029-05-13");

        SQLException error = assertThrows(SQLException.class, () ->
                insertarLinea(nuevaReserva(codigo(), "2029-05-12", "2029-05-14"), HAB_101, "2029-05-12", "2029-05-14"));

        assertEquals("23P01", error.getSQLState(),
                () -> "Se esperaba exclusion_violation (23P01), llegó: " + error.getMessage());
        assertTrue(error.getMessage().contains("rh_sin_solape"),
                () -> "El error debe nombrar la restricción: " + error.getMessage());
    }

    @Test
    @DisplayName("El día de salida se puede reutilizar como día de entrada")
    void permiteFechasContiguas() throws SQLException {
        insertarLinea(nuevaReserva(codigo(), "2029-06-01", "2029-06-03"), HAB_101, "2029-06-01", "2029-06-03");
        insertarLinea(nuevaReserva(codigo(), "2029-06-03", "2029-06-05"), HAB_101, "2029-06-03", "2029-06-05");
    }

    @Test
    @DisplayName("Cancelar la reserva libera el inventario")
    void cancelacionLiberaInventario() throws SQLException {
        UUID reserva = nuevaReserva(codigo(), "2029-07-01", "2029-07-04");
        insertarLinea(reserva, HAB_101, "2029-07-01", "2029-07-04");

        try (Connection c = pool.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "UPDATE reserva SET estado = 'CANCELADA', fecha_cancelacion = now() WHERE id = ?")) {
            ps.setObject(1, reserva);
            assertEquals(1, ps.executeUpdate());
        }

        insertarLinea(nuevaReserva(codigo(), "2029-07-02", "2029-07-03"), HAB_101, "2029-07-02", "2029-07-03");
    }

    @Test
    @DisplayName("M1: 100 hilos compiten por la última habitación y solo 1 reserva se confirma")
    void concurrenciaCienHilos() throws Exception {
        final int hilos = 100;
        final String entrada = "2031-09-10";
        final String salida = "2031-09-14";
        CyclicBarrier barrera = new CyclicBarrier(hilos);
        AtomicInteger contador = new AtomicInteger();
        ExecutorService ejecutor = Executors.newFixedThreadPool(hilos);
        List<Future<Resultado>> futuros = new ArrayList<>();

        try {
            for (int i = 0; i < hilos; i++) {
                Callable<Resultado> tarea = () -> {
                    barrera.await(30, TimeUnit.SECONDS);
                    try (Connection c = pool.getConnection()) {
                        c.setAutoCommit(false);
                        try {
                            UUID reserva = insertarReserva(c, codigo(contador.incrementAndGet()), entrada, salida);
                            insertarLinea(c, reserva, HAB_101, entrada, salida);
                            c.commit();
                            return Resultado.CONFIRMADA;
                        } catch (SQLException e) {
                            c.rollback();
                            if ("23P01".equals(e.getSQLState())) {
                                return Resultado.RECHAZADA;
                            }
                            return Resultado.ERROR;
                        }
                    }
                };
                futuros.add(ejecutor.submit(tarea));
            }

            long confirmadas = 0;
            long rechazadas = 0;
            long errores = 0;
            for (Future<Resultado> futuro : futuros) {
                switch (futuro.get(60, TimeUnit.SECONDS)) {
                    case CONFIRMADA -> confirmadas++;
                    case RECHAZADA -> rechazadas++;
                    case ERROR -> errores++;
                }
            }

            assertEquals(0, errores, "errores distintos de la violación de exclusión");
            assertEquals(1, confirmadas, "exactamente una reserva debe ganar la carrera");
            assertEquals(hilos - 1, rechazadas, "el resto debe rechazarse con 23P01");
        } finally {
            ejecutor.shutdownNow();
        }
    }

    // ---------------------------------------------------------------- ayudas

    private enum Resultado {
        CONFIRMADA, RECHAZADA, ERROR
    }

    private static String codigo() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private static String codigo(int secuencia) {
        return String.format("C%07d", secuencia % 10_000_000);
    }

    private static long contar(Connection c, String tabla) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT count(*) FROM " + tabla);
                java.sql.ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private static UUID nuevaReserva(String codigo, String entrada, String salida) throws SQLException {
        try (Connection c = pool.getConnection()) {
            return insertarReserva(c, codigo, entrada, salida);
        }
    }

    private static UUID insertarReserva(Connection c, String codigo, String entrada, String salida)
            throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO reserva (id, propiedad_id, titular_id, canal_id, politica_cancelacion_id,
                                     codigo_confirmacion, fecha_entrada, fecha_salida, adultos, ninos,
                                     estado, descuento_porcentaje, total)
                VALUES (?, ?, ?, ?, ?, ?, ?::date, ?::date, 2, 0, 'CONFIRMADA', 0, 0)
                """)) {
            ps.setObject(1, id);
            ps.setObject(2, UUID.fromString(PROPIEDAD));
            ps.setObject(3, UUID.fromString(HUESPED));
            ps.setObject(4, UUID.fromString(CANAL));
            ps.setObject(5, UUID.fromString(POLITICA));
            ps.setString(6, codigo);
            ps.setString(7, entrada);
            ps.setString(8, salida);
            ps.executeUpdate();
        }
        return id;
    }

    private static void insertarLinea(UUID reserva, String habitacion, String entrada, String salida)
            throws SQLException {
        try (Connection c = pool.getConnection()) {
            insertarLinea(c, reserva, habitacion, entrada, salida);
        }
    }

    private static void insertarLinea(Connection c, UUID reserva, String habitacion, String entrada, String salida)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO reserva_habitacion (id, reserva_id, habitacion_id, tipo_habitacion_id,
                                                tarifa_id, fecha_entrada, fecha_salida, precio_total)
                VALUES (?, ?, ?, ?, ?, ?::date, ?::date, 0)
                """)) {
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, reserva);
            ps.setObject(3, UUID.fromString(habitacion));
            ps.setObject(4, UUID.fromString(TIPO_DOBLE));
            ps.setObject(5, UUID.fromString(TARIFA));
            ps.setString(6, entrada);
            ps.setString(7, salida);
            ps.executeUpdate();
        }
    }

    private static String entorno(String nombre, String porDefecto) {
        String valor = System.getenv(nombre);
        return valor == null || valor.isBlank() ? porDefecto : valor;
    }
}
