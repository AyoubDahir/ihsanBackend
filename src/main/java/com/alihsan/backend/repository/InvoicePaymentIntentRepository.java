package com.alihsan.backend.repository;

import com.alihsan.backend.domain.InvoicePaymentIntent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvoicePaymentIntentRepository extends JpaRepository<InvoicePaymentIntent, Long> {
    Optional<InvoicePaymentIntent> findByReferenceId(String referenceId);
}
