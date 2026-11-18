package com.hotel.pms.salud;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * El criterio de done del Sprint 0 exige un endpoint {@code /health}
 * funcionando. Sin datasource configurado debe responder 200 y declarar la base
 * como {@code NO_CONFIGURADA} en lugar de fallar.
 */
@WebMvcTest(SaludController.class)
class SaludControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("GET /health responde UP aunque no haya base de datos")
    void saludSinBaseDeDatos() throws Exception {
        mvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("UP"))
                .andExpect(jsonPath("$.aplicacion").value("pms-backend"))
                .andExpect(jsonPath("$.baseDatos.estado").value("NO_CONFIGURADA"));
    }
}
