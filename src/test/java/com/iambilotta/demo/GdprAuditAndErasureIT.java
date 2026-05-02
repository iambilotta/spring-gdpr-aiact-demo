package com.iambilotta.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Asserts the spring-gdpr library produces the runtime evidence the README claims:
 * a personal-data read fires the audit advisor, the access record is queryable via
 * the Article 15 export endpoint, and the Article 17 erasure flow physically removes
 * the row. This is the test that breaks if the @GdprPersonalData advisor stops
 * weaving, if the audit sink stops persisting, or if the erasure REST shape changes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GdprAuditAndErasureIT {

    @Autowired
    private MockMvc mvc;

    @Test
    void readingASubjectAppendsToTheAccessAuditLog() throws Exception {
        mvc.perform(post("/customers")
                        .contentType("application/json")
                        .content("""
                                {"id":"sub-gdpr-1","email":"alice@example.com","fullName":"Alice"}
                                """))
                .andExpect(status().isOk());

        mvc.perform(get("/customers/sub-gdpr-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("sub-gdpr-1"));

        mvc.perform(get("/gdpr/audit/access").param("subjectId", "sub-gdpr-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].subjectId").value("sub-gdpr-1"))
                .andExpect(jsonPath("$[0].targetType").value("com.iambilotta.demo.CustomerService"))
                .andExpect(jsonPath("$[0].targetMember").value("read"));
    }

    @Test
    void erasureRemovesTheRowAndReturnsTheAffectedCount() throws Exception {
        mvc.perform(post("/customers")
                        .contentType("application/json")
                        .content("""
                                {"id":"sub-gdpr-2","email":"bob@example.com","fullName":"Bob"}
                                """))
                .andExpect(status().isOk());

        mvc.perform(delete("/gdpr/erasure/sub-gdpr-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjectId").value("sub-gdpr-2"))
                .andExpect(jsonPath("$.affectedByType['com.iambilotta.demo.Customer']").value(1));

        // The row is gone. The controller's @ExceptionHandler maps the service's
        // IllegalArgumentException to a 404 with a JSON body.
        mvc.perform(get("/customers/sub-gdpr-2"))
                .andExpect(status().isNotFound());
    }
}
