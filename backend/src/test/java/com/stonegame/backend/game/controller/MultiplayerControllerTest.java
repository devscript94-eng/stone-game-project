package com.stonegame.backend.game.controller;

import com.stonegame.backend.auth.config.JwtAuthenticationFilter;
import com.stonegame.backend.common.GlobalExceptionHandler;
import com.stonegame.backend.config.SecurityConfig;
import com.stonegame.backend.game.dto.JoinMatchResponse;
import com.stonegame.backend.game.dto.MultiplayerMatchResponse;
import com.stonegame.backend.game.dto.PlayMoveRequest;
import com.stonegame.backend.game.model.MatchStatus;
import com.stonegame.backend.game.model.Move;
import com.stonegame.backend.game.model.MultiplayerResult;
import com.stonegame.backend.game.service.MatchmakingService;
import com.stonegame.backend.game.service.MultiplayerService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = MultiplayerController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class MultiplayerControllerTest {

    @Resource
    private MockMvc mockMvc;

    @MockitoBean
    private MatchmakingService matchmakingService;

    @MockitoBean
    private MultiplayerService multiplayerService;

    @Autowired
    private JsonMapper jsonMapper;

    private UsernamePasswordAuthenticationToken auth() {
        User user = new User();
        user.setId("user-123");
        user.setUsername("john");
        user.setEmail("john@company.com");
        user.setRole(Role.USER);
        return new UsernamePasswordAuthenticationToken(user, null, null);
    }

    @Test
    @DisplayName("POST /multiplayer/join should return matchmaking response")
    void join_shouldReturnMatchmakingResponse() throws Exception {
        JoinMatchResponse response =
                new JoinMatchResponse("match-1", MatchStatus.WAITING_FOR_PLAYER, "Waiting for another player");

        when(matchmakingService.join(any(User.class))).thenReturn(response);

        mockMvc.perform(post("/multiplayer/join")
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value("match-1"))
                .andExpect(jsonPath("$.status").value("WAITING_FOR_PLAYER"))
                .andExpect(jsonPath("$.message").value("Waiting for another player"));
    }

    @Test
    @DisplayName("GET /multiplayer/{matchId} should return match state")
    void getMatch_shouldReturnMatchState() throws Exception {
        MultiplayerMatchResponse response = new MultiplayerMatchResponse(
                "match-1",
                "john",
                "jane",
                Move.STONE,
                Move.SCISSORS,
                MatchStatus.COMPLETED,
                MultiplayerResult.PLAYER_ONE_WIN
        );

        when(multiplayerService.getMatch(eq("match-1"), any(User.class))).thenReturn(response);

        mockMvc.perform(get("/multiplayer/match-1")
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value("match-1"))
                .andExpect(jsonPath("$.playerOneUsername").value("john"))
                .andExpect(jsonPath("$.playerTwoUsername").value("jane"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.result").value("PLAYER_ONE_WIN"));
    }

    @Test
    @DisplayName("POST /multiplayer/{matchId}/move should return updated match state")
    void submitMove_shouldReturnUpdatedMatchState() throws Exception {
        PlayMoveRequest request = new PlayMoveRequest();
        request.setMove(Move.PAPER);

        MultiplayerMatchResponse response = new MultiplayerMatchResponse(
                "match-1",
                "john",
                "jane",
                Move.PAPER,
                null,
                MatchStatus.WAITING_FOR_MOVES,
                null
        );

        when(multiplayerService.submitMove(eq("match-1"), any(User.class), eq(Move.PAPER))).thenReturn(response);

        mockMvc.perform(post("/multiplayer/match-1/move")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value("match-1"))
                .andExpect(jsonPath("$.playerOneMove").value("PAPER"))
                .andExpect(jsonPath("$.status").value("WAITING_FOR_MOVES"));
    }

    @Test
    @DisplayName("POST /multiplayer/{matchId}/move should return 400 when move is missing")
    void submitMove_shouldReturnBadRequestWhenMoveMissing() throws Exception {
        PlayMoveRequest request = new PlayMoveRequest();

        mockMvc.perform(post("/multiplayer/match-1/move")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.move").exists());
    }
}