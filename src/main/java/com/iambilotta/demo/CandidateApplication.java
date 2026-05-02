package com.iambilotta.demo;

public class CandidateApplication {
    private String candidateId;
    private String cvText;

    public CandidateApplication() {}

    public String getCandidateId() { return candidateId; }
    public void setCandidateId(String candidateId) { this.candidateId = candidateId; }

    public String getCvText() { return cvText; }
    public void setCvText(String cvText) { this.cvText = cvText; }

    public String candidateId() { return candidateId; }
    public String cvText() { return cvText; }
}
