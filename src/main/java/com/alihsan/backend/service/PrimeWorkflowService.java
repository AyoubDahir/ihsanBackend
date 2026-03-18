package com.alihsan.backend.service;

import com.alihsan.backend.dto.LabReportView;
import com.alihsan.backend.dto.PatientAppointmentView;
import com.alihsan.backend.integration.FrappeClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PrimeWorkflowService {
    private final FrappeClient frappeClient;

    public PrimeWorkflowService(FrappeClient frappeClient) {
        this.frappeClient = frappeClient;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> createQueFromMobile(
        String patientId,
        String practitionerId,
        String appointmentDate,
        String appointmentTime,
        String department,
        String referenceId
    ) {
        return frappeClient.postMethod(
            "prime.mobile_api.create_que_from_mobile",
            Map.of(
                "patient", patientId,
                "practitioner", practitionerId,
                "appointment_date", appointmentDate,
                "appointment_time", appointmentTime,
                "department", department,
                "reference_id", referenceId
            )
        );
    }

    @SuppressWarnings("unchecked")
    public List<PatientAppointmentView> getAppointments(String patientId) {
        Map<String, Object> response = frappeClient.getResource(
            "Patient Appointment",
            Map.of(
                "fields", "[\"name\",\"patient\",\"practitioner\",\"practitioner_name\",\"appointment_date\",\"appointment_time\",\"status\",\"department\"]",
                "filters", "[[\"patient\",\"=\",\"" + patientId + "\"]]",
                "order_by", "appointment_date desc"
            )
        );
        List<Map<String, Object>> rows = (List<Map<String, Object>>) response.getOrDefault("data", List.of());
        List<PatientAppointmentView> mapped = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            mapped.add(new PatientAppointmentView(
                asString(row.get("name")),
                asString(row.get("patient")),
                asString(row.get("practitioner")),
                asString(row.get("practitioner_name")),
                asString(row.get("appointment_date")),
                asString(row.get("appointment_time")),
                asString(row.get("status")),
                asString(row.get("department"))
            ));
        }
        return mapped;
    }

    @SuppressWarnings("unchecked")
    public List<LabReportView> getLabReports(String patientId) {
        Map<String, Object> response = frappeClient.postMethod(
            "prime.mobile_api.get_lab_reports_for_mobile",
            Map.of("patient", patientId, "limit", 50)
        );
        List<Map<String, Object>> rows = (List<Map<String, Object>>) response.getOrDefault("message", List.of());
        List<LabReportView> mapped = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            mapped.add(new LabReportView(
                asString(row.get("name")),
                asString(row.get("lab_test_name")),
                asString(row.get("status")),
                asString(row.get("date")),
                asString(row.get("download_url"))
            ));
        }
        return mapped;
    }

    private String asString(Object val) {
        return val == null ? null : String.valueOf(val);
    }
}
