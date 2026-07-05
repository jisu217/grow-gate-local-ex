package com.growgate.publicdata.controller;

import com.growgate.config.SecurityConfig;
import com.growgate.publicdata.service.PublicDataProofRunService;
import com.growgate.security.JwtAccessDeniedHandler;
import com.growgate.security.JwtAuthenticationEntryPoint;
import com.growgate.security.JwtTokenProvider;
import com.growgate.security.TokenRevocationService;
import com.growgate.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicDataProofController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class PublicDataProofControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PublicDataProofRunService proofRunService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private TokenRevocationService tokenRevocationService;

    @Test
    @WithMockUser(username = "admin-user", authorities = "ROLE_ADMIN")
    void adminCanCreateProofRun() throws Exception {
        when(proofRunService.createProofRun(eq("admin-user"), eq("manual")))
                .thenReturn(Map.of("proofStatus", "PASS"));

        mockMvc.perform(post("/api/admin/public-data/proof-runs"))
                .andExpect(status().isAccepted());
    }

    @Test
    @WithMockUser(username = "owner-user", authorities = "ROLE_OWNER")
    void ownerCanCreateProofRun() throws Exception {
        when(proofRunService.createProofRun(eq("owner-user"), eq("manual")))
                .thenReturn(Map.of("proofStatus", "PASS"));

        mockMvc.perform(post("/api/admin/public-data/proof-runs"))
                .andExpect(status().isAccepted());
    }

    @Test
    @WithMockUser(username = "editor-user", authorities = "ROLE_EDITOR")
    void editorCannotCreateProofRun() throws Exception {
        mockMvc.perform(post("/api/admin/public-data/proof-runs"))
                .andExpect(status().isForbidden());
    }
}
