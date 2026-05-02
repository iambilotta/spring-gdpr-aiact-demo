package com.iambilotta.demo;

import com.iambilotta.gdpr.annotations.GdprPersonalData;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository repo;

    public CustomerService(CustomerRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public Customer create(String id, String email, String fullName) {
        return repo.save(new Customer(id, email, fullName));
    }

    @GdprPersonalData(description = "subject access: read of personal record by subject id")
    @Transactional(readOnly = true)
    public Customer read(@GdprPersonalData String subjectId) {
        return repo.findById(subjectId).orElseThrow(
                () -> new IllegalArgumentException("subject not found: " + subjectId));
    }

    @Transactional
    public int hardDelete(String subjectId) {
        if (!repo.existsById(subjectId)) return 0;
        repo.deleteById(subjectId);
        return 1;
    }
}
