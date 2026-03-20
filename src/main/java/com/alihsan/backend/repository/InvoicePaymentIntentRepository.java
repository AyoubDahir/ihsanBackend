package com.alihsan.backend.repository;

import com.alihsan.backend.domain.InvoicePaymentIntent;
import com.alihsan.backend.domain.InvoicePaymentIntentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface InvoicePaymentIntentRepository extends JpaRepository<InvoicePaymentIntent, Long> {
    Optional<InvoicePaymentIntent> findByReferenceId(String referenceId);
    List<InvoicePaymentIntent> findByStatusAndCreatedAtBefore(InvoicePaymentIntentStatus status, OffsetDateTime before);
}
