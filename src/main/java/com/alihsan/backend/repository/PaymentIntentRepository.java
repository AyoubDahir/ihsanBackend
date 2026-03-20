package com.alihsan.backend.repository;

import com.alihsan.backend.domain.PaymentIntent;
import com.alihsan.backend.domain.PaymentIntentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentIntentRepository extends JpaRepository<PaymentIntent, Long> {
    Optional<PaymentIntent> findByReferenceId(String referenceId);
    List<PaymentIntent> findByStatusAndCreatedAtBefore(PaymentIntentStatus status, OffsetDateTime before);
}
