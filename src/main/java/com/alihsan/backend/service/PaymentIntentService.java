package com.alihsan.backend.service;

import com.alihsan.backend.domain.PaymentIntent;
import com.alihsan.backend.domain.PaymentIntentStatus;
import com.alihsan.backend.dto.CreateAppointmentIntentRequest;
import com.alihsan.backend.repository.PaymentIntentRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentIntentService {
    private final PaymentIntentRepository paymentIntentRepository;
    private final PaymentProviderService paymentProviderService;
    private final PrimeWorkflowService primeWorkflowService;

    public PaymentIntentService(
        PaymentIntentRepository paymentIntentRepository,
        PaymentProviderService paymentProviderService,
        PrimeWorkflowService primeWorkflowService
    ) {
        this.paymentIntentRepository = paymentIntentRepository;
        this.paymentProviderService = paymentProviderService;
        this.primeWorkflowService = primeWorkflowService;
    }

    @Transactional
    public PaymentIntent createIntent(CreateAppointmentIntentRequest request) {
        String referenceId = "APPT-" + UUID.randomUUID();
        String patientId = resolvePatientId(request);

        PaymentIntent intent = new PaymentIntent();
        intent.setReferenceId(referenceId);
        intent.setPatientId(patientId);
        intent.setPractitionerId(request.practitionerId());
        intent.setAppointmentDate(request.appointmentDate());
        intent.setAppointmentTime(request.appointmentTime());
        intent.setDepartment(request.department());
        intent.setAmount(request.amount());
        intent.setStatus(PaymentIntentStatus.PENDING);

        paymentIntentRepository.save(intent);
        try {
            paymentProviderService.initiatePayment(referenceId, request.amount(), request.phoneNumber(), request.paymentType());
        } catch (Exception ex) {
            intent.setStatus(PaymentIntentStatus.FAILED);
            paymentIntentRepository.save(intent);
            throw ex;
        }
        return intent;
    }

    @Transactional
    public PaymentIntent handleWebhook(String referenceId, String providerStatus) {
        PaymentIntent intent = paymentIntentRepository.findByReferenceId(referenceId)
            .orElseThrow(() -> new IllegalArgumentException("Payment intent not found: " + referenceId));

        if (intent.getStatus() == PaymentIntentStatus.APPOINTMENT_CREATED) {
            return intent;
        }

        if ("APPROVED".equalsIgnoreCase(providerStatus)) {
            intent.setStatus(PaymentIntentStatus.APPROVED);
            Map<String, Object> result = primeWorkflowService.createQueFromMobile(
                intent.getPatientId(),
                intent.getPractitionerId(),
                intent.getAppointmentDate(),
                intent.getAppointmentTime(),
                intent.getDepartment(),
                intent.getReferenceId()
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) result.getOrDefault("message", Map.of());
            intent.setPrimeQue(asString(message.get("que")));
            intent.setPrimeInvoice(asString(message.get("invoice")));
            intent.setStatus(PaymentIntentStatus.APPOINTMENT_CREATED);
        } else {
            intent.setStatus(PaymentIntentStatus.FAILED);
        }

        return paymentIntentRepository.save(intent);
    }

    public Optional<PaymentIntent> findByReference(String referenceId) {
        return paymentIntentRepository.findByReferenceId(referenceId);
    }

    private String asString(Object val) {
        return val == null ? null : String.valueOf(val);
    }

    private String resolvePatientId(CreateAppointmentIntentRequest request) {
        String patientId = request.patientId();
        if (patientId != null && !patientId.trim().isEmpty()) {
            return patientId.trim();
        }
        Map<String, String> existingPatient = primeWorkflowService.findPatientByMobile(request.mobile());
        if (existingPatient != null) {
            String existingPatientId = existingPatient.get("id");
            if (existingPatientId != null && !existingPatientId.trim().isEmpty()) {
                return existingPatientId.trim();
            }
        }
        return primeWorkflowService.registerPatientFromMobile(
            request.firstName(),
            request.lastName(),
            request.fullName(),
            request.mobile(),
            request.sex(),
            request.age(),
            request.ageType()
        );
    }
}
