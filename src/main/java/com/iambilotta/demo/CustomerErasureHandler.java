package com.iambilotta.demo;

import com.iambilotta.gdpr.annotations.GdprErasable;
import com.iambilotta.gdpr.starter.erasure.ErasureHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CustomerErasureHandler implements ErasureHandler {

    private final CustomerService service;

    public CustomerErasureHandler(CustomerService service) {
        this.service = service;
    }

    @Override
    public Class<?> entityType() {
        return Customer.class;
    }

    @Override
    public GdprErasable.Strategy strategy() {
        return GdprErasable.Strategy.DELETE;
    }

    @Override
    @Transactional
    public int erase(String subjectId) {
        return service.hardDelete(subjectId);
    }

    @Override
    public int order() {
        return 10;
    }
}
