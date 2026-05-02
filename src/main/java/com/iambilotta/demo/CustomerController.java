package com.iambilotta.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    public static class CreateRequest {
        private String id;
        private String email;
        private String fullName;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
    }

    @PostMapping
    public Customer create(@RequestBody CreateRequest req) {
        return service.create(req.getId(), req.getEmail(), req.getFullName());
    }

    @GetMapping("/{id}")
    public Customer read(@PathVariable String id) {
        return service.read(id);
    }
}
