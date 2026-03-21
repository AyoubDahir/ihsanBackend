package com.alihsan.backend.controller;

import com.alihsan.backend.dto.CreateAppointmentIntentRequest;
import com.alihsan.backend.dto.CreateAppointmentIntentResponse;
import com.alihsan.backend.dto.LabReportView;
import com.alihsan.backend.dto.QueueStatusView;
import com.alihsan.backend.dto.PatientAppointmentView;
import com.alihsan.backend.dto.PractitionerSlotView;
import com.alihsan.backend.dto.PractitionerView;
import com.alihsan.backend.service.PaymentIntentService;
import com.alihsan.backend.service.PrimeWorkflowService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/practitioners")
    public List<PractitionerView> getPractitioners(@RequestParam(value = "department", required = false) String department) {
        return primeWorkflowService.getPractitioners(department);
    }

    @GetMapping("/practitioners/{practitionerId}/slots")
    public List<PractitionerSlotView> getPractitionerSlots(
        @PathVariable("practitionerId") @NotBlank String practitionerId,
        @RequestParam("date") @NotBlank String date
    ) {
        return primeWorkflowService.getPractitionerSlots(practitionerId, date);
    }

    @GetMapping("/lab-reports")
    public List<LabReportView> getLabReports(@RequestParam("patientId") String patientId) {
        return primeWorkflowService.getLabReports(patientId);
    }

    @GetMapping("/queue/status")
    public QueueStatusView getQueueStatus(@RequestParam("queId") String queId) {
        return primeWorkflowService.getQueueStatus(queId);
    }
}
