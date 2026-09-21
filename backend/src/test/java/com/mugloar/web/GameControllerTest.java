package com.mugloar.web;

import com.mugloar.DragonTrainerApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = DragonTrainerApplication.class)
class GameControllerTest {
    @Autowired
    private WebApplicationContext applicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
    }

    @Test
    void rejectsMalformedGameIdsAtTheHttpBoundary() throws Exception {
        mockMvc.perform(get("/api/games/invalid!"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    void mapsUnknownSessionsToProblemDetails() throws Exception {
        mockMvc.perform(get("/api/games/missing-game"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Game not found"));
    }

    @Test
    void rejectsUnknownStrategyModesWithAReadableProblem() throws Exception {
        mockMvc.perform(put("/api/games/valid-game/strategy-mode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"CHAOS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }
}
