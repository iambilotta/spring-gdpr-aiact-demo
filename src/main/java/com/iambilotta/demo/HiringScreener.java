package com.iambilotta.demo;

import com.iambilotta.spring.aiact.annotation.AiActAccuracyMetric;
import com.iambilotta.spring.aiact.annotation.AiActDataset;
import com.iambilotta.spring.aiact.annotation.AiActHighRiskSystem;
import com.iambilotta.spring.aiact.annotation.AiActIntendedPurpose;
import com.iambilotta.spring.aiact.annotation.AiActLog;
import com.iambilotta.spring.aiact.annotation.AiActOversight;
import com.iambilotta.spring.aiact.annotation.AnnexIIICategory;
import com.iambilotta.spring.aiact.annotation.OversightLevel;
import com.iambilotta.spring.aiact.annotation.RiskMetric;
import org.springframework.stereotype.Service;

@Service
@AiActHighRiskSystem(
        id = "hiring-screener",
        name = "Hiring screener (demo)",
        category = AnnexIIICategory.EMPLOYMENT_AND_WORKERS_MANAGEMENT,
        annexSubpoint = "4(a)",
        intendedPurpose = "Score CV applicants for an engineering role.",
        provider = "iambilotta demo",
        version = "0.1.0"
)
@AiActIntendedPurpose(
        deploymentContext = "HR triage before any human review.",
        users = {"HR specialists"},
        foreseeableMisuse = {
                "Auto-rejection without human review",
                "Use outside the engineering role context"
        },
        geographies = {"EU"},
        languages = {"it", "en"}
)
@AiActOversight(
        level = OversightLevel.HUMAN_IN_THE_LOOP,
        description = "Every output is reviewed by an HR specialist before action.",
        overrideRole = "hr"
)
@AiActDataset(
        id = "cv-corpus-2025",
        name = "Anonymized CV corpus 2025",
        phase = "training",
        source = "internal-s3://cv-2025",
        size = "12,500 records",
        license = "internal",
        biases = {"under-representation of women in STEM"},
        personalData = true
)
@AiActAccuracyMetric(
        metric = RiskMetric.PRECISION,
        threshold = ">=0.92",
        harness = "src/test/.../HiringScreenerMetricTest.java",
        population = "all"
)
public class HiringScreener {

    @AiActLog(modelId = "hiring-screener@0.1.0")
    public ScoringResult score(CandidateApplication application) {
        double s = Math.min(1.0, application.cvText().length() / 1000.0);
        String hint = s >= 0.6 ? "FORWARD_TO_HR" : "MANUAL_REVIEW_REQUIRED";
        return new ScoringResult(application.candidateId(), s, hint);
    }
}
