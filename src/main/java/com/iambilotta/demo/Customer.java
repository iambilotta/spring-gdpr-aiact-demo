package com.iambilotta.demo;

import com.iambilotta.gdpr.annotations.GdprDataSubjects;
import com.iambilotta.gdpr.annotations.GdprErasable;
import com.iambilotta.gdpr.annotations.GdprLegalBasis;
import com.iambilotta.gdpr.annotations.GdprPersonalData;
import com.iambilotta.gdpr.annotations.GdprRetention;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;

@Entity
@GdprDataSubjects(categories = {"customer"})
@GdprLegalBasis(
        value = GdprLegalBasis.LawfulBasis.CONTRACT,
        article = "6(1)(b)"
)
@GdprRetention(period = "P5Y", strategy = GdprRetention.Strategy.ANONYMIZE)
@GdprErasable(strategy = GdprErasable.Strategy.DELETE, subjectIdField = "id", order = 10)
public class Customer {

    @Id
    private String id;

    @GdprPersonalData(description = "primary contact email")
    private String email;

    @GdprPersonalData(description = "full legal name on contract")
    private String fullName;

    private Instant createdAt = Instant.now();

    protected Customer() {
    }

    public Customer(String id, String email, String fullName) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void anonymize() {
        this.email = "anonymized@local";
        this.fullName = "ANONYMIZED";
    }
}
