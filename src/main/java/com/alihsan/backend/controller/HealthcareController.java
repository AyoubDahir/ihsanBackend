package com.alihsan.backend.controller;

import com.alihsan.backend.dto.CreateAppointmentIntentRequest;
import com.alihsan.backend.dto.CreateAppointmentIntentResponse;
import com.alihsan.backend.dto.LabReportView;
import com.alihsan.backend.dto.PatientAppointmentView;
import com.alihsan.backend.service.PaymentIntentService;
import com.alihsan.backend.service.PrimeWorkflowService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/healthcare")
public class HealthcareController {
    private final PaymentIntentService paymentIntentService;
    private final PrimeWorkflowService primeWorkflowService;

    public HealthcareController(PaymentIntentService paymentIntentService, PrimeWorkflowService primeWorkflowService) {
        this.paymentIntentService = paymentIntentService;
        this.primeWorkflowService = primeWorkflowService;
    }

    @PostMapping("/appointments")
    public CreateAppointmentIntentResponse createAppointmentIntent(@Valid @RequestBody CreateAppointmentIntentRequest request) {
        var intent = paymentIntentService.createIntent(request);
        return new CreateAppointmentIntentResponse(
            intent.getReferenceId(),
            intent.getStatus().name(),
            "Payment initiated. Wait for webhook confirmation."
        );
    }

    @GetMapping("/appointments")
    public List<PatientAppointmentView> getAppointments(@RequestParam("patientId") String patientId) {
        return primeWorkflowService.getAppointments(patientId);
    }

    @GetMapping("/lab-reports")
    public List<LabReportView> getLabReports(@RequestParam("patientId") String patientId) {
        return primeWorkflowService.getLabReports(patientId);
    }
}
