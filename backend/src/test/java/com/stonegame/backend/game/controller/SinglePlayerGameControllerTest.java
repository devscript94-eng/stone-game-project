package com.stonegame.backend.game.controller;

import com.stonegame.backend.auth.config.JwtAuthenticationFilter;
import com.stonegame.backend.common.GlobalExceptionHandler;
import com.stonegame.backend.config.SecurityConfig;
import com.stonegame.backend.game.dto.PlayMoveRequest;
import com.stonegame.backend.game.dto.SinglePlayerGameResponse;
import com.stonegame.backend.game.model.GameResult;
import com.stonegame.backend.game.model.Move;
import com.stonegame.backend.game.service.SinglePlayerGameService;
import com.stonegame.backend.user.model.Role;
import com.stonegame.backend.user.model.User;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.beans.factory.annotation.Autowired;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = SinglePlayerGameController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class SinglePlayerGameControllerTest {

    @Resource
    private MockMvc mockMvc;

    @MockitoBean
    private SinglePlayerGameService singlePlayerGameService;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    @DisplayName("POST /games/single-player/play should return 200 and game response")
    void play_shouldReturnGameResponse() throws Exception {
        User user = new User();
        user.setId("user-123");
        user.setUsername("john");
        user.setEmail("john@company.com");
        user.setRole(Role.USER);

        var auth = new UsernamePasswordAuthenticationToken(user, null, null);

        PlayMoveRequest request = new PlayMoveRequest();
        request.setMove(Move.STONE);

        SinglePlayerGameResponse response =
                new SinglePlayerGameResponse("game-123", Move.STONE, Move.SCISSORS, GameResult.WIN);

        when(singlePlayerGameService.play(any(User.class), eq(Move.STONE))).thenReturn(response);

        mockMvc.perform(post("/games/single-player/play")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value("game-123"))
                .andExpect(jsonPath("$.playerMove").value("STONE"))
                .andExpect(jsonPath("$.computerMove").value("SCISSORS"))
                .andExpect(jsonPath("$.result").value("WIN"));
    }

    @Test
    @DisplayName("POST /games/single-player/play should return 400 when move is missing")
    void play_shouldReturnBadRequestWhenMoveMissing() throws Exception {
        PlayMoveRequest request = new PlayMoveRequest();

        mockMvc.perform(post("/games/single-player/play")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.move").exists());
    }
}