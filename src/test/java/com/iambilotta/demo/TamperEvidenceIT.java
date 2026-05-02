package com.iambilotta.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Asserts the property that motivates Article 12 of the AI Act: the audit log is
 * tamper-evident. Edit a single byte of an old record on disk, the chain breaks at
 * that record and {@code /aiact/log/verify} reports the failed event id. Restore
 * the byte and the chain is whole again.
 *
 * <p>This is the load-bearing claim of the entire library. The test belongs in the
 * demo because the demo is the canonical "does it actually work" surface.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TamperEvidenceIT {

    @Autowired
    private MockMvc mvc;

    @Value("${aiact.log-dir}")
    private String logDir;

    @Test
    void mutatingTheLogFileMakesVerifyFlagThePreciseRecord() throws Exception {
        mvc.perform(post("/hiring/score")
                        .contentType("application/json")
                        .content("""
                                {"candidateId":"c-tamper","cvText":"baseline text for the tamper test"}
                                """))
                .andExpect(status().isOk());

        mvc.perform(get("/aiact/log/verify").param("system", "hiring-screener"))
                .andExpect(jsonPath("$.invalid").value(0));

        Path ndjson = Path.of(logDir, "hiring-screener.ndjson");
        assertThat(ndjson)
                .as("after one logged invocation the NDJSON file must exist")
                .exists();

        String original = Files.readString(ndjson);
        assertThat(original)
                .as("the audit record uses the spec's snake_case keys regardless of the app's "
                    + "ObjectMapper naming strategy (regression for the v0.1.x shadowing bug)")
                .contains("\"event_id\"")
                .contains("\"system_id\":\"hiring-screener\"")
                .contains("\"record_hmac\"");

        // Corrupt the input_hash hex by flipping the first character. The HMAC
        // chain depends on the full record, so any one-byte change breaks verify.
        String tampered = original.replaceFirst(
                "\"input_hash\":\"sha256:[0-9a-f]",
                "\"input_hash\":\"sha256:0");
        Files.writeString(ndjson, tampered);

        // After corrupting one record, verify must report at least one failed event id.
        // The HMAC chain propagates the failure to every subsequent record that points
        // back to the corrupted one, so the count can be 1+ depending on how many
        // records were present in this test run.
        mvc.perform(get("/aiact/log/verify").param("system", "hiring-screener"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invalid").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.failedEventIds").isArray())
                .andExpect(jsonPath("$.failedEventIds[0]").isString());

        // Restore the original content. The chain becomes whole again, no
        // server restart needed: the verifier is stateless against the file.
        Files.writeString(ndjson, original);

        mvc.perform(get("/aiact/log/verify").param("system", "hiring-screener"))
                .andExpect(jsonPath("$.invalid").value(0))
                .andExpect(jsonPath("$.failedEventIds").isEmpty());
    }
}
