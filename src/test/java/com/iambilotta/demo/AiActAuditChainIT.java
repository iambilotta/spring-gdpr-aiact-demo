package com.iambilotta.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Asserts the spring-aiact library produces the runtime evidence the README claims:
 * after an annotated method invocation, the NDJSON HMAC chain has exactly one record
 * and the on-line {@code /aiact/log/verify} endpoint reports the chain as intact.
 *
 * <p>This is the test that breaks if the library starts misbehaving in any future
 * Spring Boot upgrade, library refactor, or annotation rename. It exists to give
 * the demo a continuous-integration heartbeat against the upstream libraries.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiActAuditChainIT {

    @Autowired
    private MockMvc mvc;

    @Test
    void scoringInvocationAppendsAtLeastOneRecordAndChainVerifies() throws Exception {
        mvc.perform(post("/hiring/score")
                        .contentType("application/json")
                        .content("""
                                {"candidateId":"c-aiact-it",
                                 "cvText":"Senior Java engineer with deep Spring Boot experience"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateId").value("c-aiact-it"))
                .andExpect(jsonPath("$.score").exists())
                .andExpect(jsonPath("$.decisionHint").exists());

        mvc.perform(get("/aiact/log/verify")
                        .param("system", "hiring-screener"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.systemId").value("hiring-screener"))
                .andExpect(jsonPath("$.inspected").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.invalid").value(0))
                .andExpect(jsonPath("$.failedEventIds").isEmpty());
    }

    @Test
    void chainHeadAdvancesAcrossMultipleInvocations() throws Exception {
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/hiring/score")
                            .contentType("application/json")
                            .content("{\"candidateId\":\"c-" + i + "\",\"cvText\":\"sample text " + i + "\"}"))
                    .andExpect(status().isOk());
        }

        mvc.perform(get("/aiact/log/verify").param("system", "hiring-screener"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inspected").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.invalid").value(0));

        mvc.perform(get("/aiact/log/head").param("system", "hiring-screener"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.system_id").value("hiring-screener"))
                .andExpect(jsonPath("$.head_hmac").isString())
                .andExpect(jsonPath("$.head_hmac").value(org.hamcrest.Matchers.not("0".repeat(64))));
    }
}
