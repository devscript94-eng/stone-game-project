package com.stonegame.backend.auth;

import com.stonegame.backend.common.GlobalExceptionHandler;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.beans.factory.annotation.Autowired;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AuthController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Resource
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    @DisplayName("POST /api/auth/register should return 201 and auth payload")
    void register_shouldReturnCreated() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("lenin");
        request.setEmail("lenin@company.com");
        request.setPassword("Password123");

        AuthResponse response = new AuthResponse(
                "jwt-token",
                "user-123",
                "lenin",
                "lenin@company.com",
                "USER"
        );

        Mockito.when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.userId").value("user-123"))
                .andExpect(jsonPath("$.username").value("lenin"))
                .andExpect(jsonPath("$.email").value("lenin@company.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("POST /api/auth/login should return 200 and auth payload")
    void login_shouldReturnOk() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("lenin@company");
        request.setPassword("Password123");

        AuthResponse response = new AuthResponse(
                "jwt-token",
                "user-123",
                "lenin",
                "lenin@company.com",
                "USER"
        );

        Mockito.when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.userId").value("user-123"));
    }

    @Test
    @DisplayName("POST /api/auth/register should return 400 when request is invalid")
    void register_shouldReturnBadRequestWhenValidationFails() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("le");
        request.setEmail("not-an-email");
        request.setPassword("123");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.username").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }
}