package com.alihsan.backend.service;

import com.alihsan.backend.dto.BillingInvoiceView;
import com.alihsan.backend.dto.LabReportView;
import com.alihsan.backend.dto.PatientAppointmentView;
import com.alihsan.backend.dto.PractitionerView;
import com.alihsan.backend.integration.FrappeClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    public Map<String, String> findPatientByMobile(String mobile) {
        if (mobile == null || mobile.trim().isEmpty()) {
            return null;
        }
        Map<String, Object> response = frappeClient.getResource(
            "Patient",
            Map.of(
                "fields", "[\"name\",\"patient_name\",\"mobile\"]",
                "filters", "[[\"mobile\",\"=\",\"" + mobile + "\"]]",
                "limit_page_length", "1"
            )
        );
        List<Map<String, Object>> rows = (List<Map<String, Object>>) response.getOrDefault("data", List.of());
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> row = rows.get(0);
        Map<String, String> out = new HashMap<>();
        out.put("id", asString(row.get("name")));
        out.put("name", asString(row.get("patient_name")));
        out.put("mobile", asString(row.get("mobile")));
        return out;
    }

    @SuppressWarnings("unchecked")
    public String registerPatientFromMobile(
        String firstName,
        String lastName,
        String fullName,
        String mobile,
        String sex,
        Integer age,
        String ageType
    ) {
        String normalizedMobile = mobile == null ? null : mobile.trim();
        if (normalizedMobile == null || normalizedMobile.isEmpty()) {
            throw new IllegalArgumentException("mobile is required when patientId is missing");
        }
        if (age == null) {
            throw new IllegalArgumentException("age is required when patientId is missing");
        }
        if (age < 0) {
            throw new IllegalArgumentException("age cannot be negative");
        }

        String normalizedSex = sex == null ? "Male" : sex.trim();
        if (normalizedSex.isEmpty()) {
            normalizedSex = "Male";
        }
        String normalizedAgeType = ageType == null ? "" : ageType.trim();
        if (normalizedAgeType.isEmpty()) {
            throw new IllegalArgumentException("ageType is required when patientId is missing");
        }
        normalizedAgeType = normalizedAgeType.substring(0, 1).toUpperCase() + normalizedAgeType.substring(1).toLowerCase();
        if (!normalizedAgeType.equals("Year") && !normalizedAgeType.equals("Month") && !normalizedAgeType.equals("Day")) {
            throw new IllegalArgumentException("ageType must be one of: Year, Month, Day");
        }

        String normalizedFullName = fullName == null ? "" : fullName.trim();
        String normalizedFirstName = firstName == null ? "" : firstName.trim();
        String normalizedLastName = lastName == null ? "" : lastName.trim();

        if (normalizedFirstName.isEmpty() && normalizedLastName.isEmpty() && !normalizedFullName.isEmpty()) {
            String[] parts = normalizedFullName.split("\\s+", 2);
            normalizedFirstName = parts[0];
            normalizedLastName = parts.length > 1 ? parts[1] : "Unknown";
        }

        if (normalizedFirstName.isEmpty()) {
            throw new IllegalArgumentException("firstName or fullName is required when patientId is missing");
        }
        if (normalizedLastName.isEmpty()) {
            normalizedLastName = "Unknown";
        }

        Map<String, Object> response = frappeClient.postMethod(
            "prime.mobile_api.register_patient_from_mobile",
            Map.of(
                "first_name", normalizedFirstName,
                "last_name", normalizedLastName,
                "mobile", normalizedMobile,
                "sex", normalizedSex,
                "p_age", age,
                "age_type", normalizedAgeType
            )
        );

        Map<String, Object> message = (Map<String, Object>) response.get("message");
        if (message == null || message.get("patient") == null) {
            throw new IllegalStateException("Prime registration response did not include patient id");
        }
        return String.valueOf(message.get("patient"));
    }

    @SuppressWarnings("unchecked")
    public List<PractitionerView> getPractitioners(String department) {
        String filters = (department == null || department.trim().isEmpty())
            ? "[]"
            : "[[\"department\",\"=\",\"" + department.trim() + "\"]]";
        Map<String, Object> response = frappeClient.getResource(
            "Healthcare Practitioner",
            Map.of(
                "fields", "[\"name\",\"practitioner_name\",\"department\",\"op_consulting_charge\",\"disabled\"]",
                "filters", filters,
                "order_by", "modified desc",
                "limit_page_length", "200"
            )
        );
        List<Map<String, Object>> rows = (List<Map<String, Object>>) response.getOrDefault("data", List.of());
        List<PractitionerView> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            boolean disabled = "1".equals(asString(row.get("disabled")))
                || "true".equalsIgnoreCase(asString(row.get("disabled")));
            out.add(new PractitionerView(
                asString(row.get("name")),
                asString(row.get("practitioner_name")),
                asString(row.get("department")),
                asString(row.get("op_consulting_charge")),
                !disabled
            ));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    public List<BillingInvoiceView> getUnpaidInvoices(String patientId) {
        Map<String, Object> response = frappeClient.postMethod(
            "prime.mobile_api.get_unpaid_sales_invoices_for_mobile",
            Map.of("patient", patientId, "limit", 100)
        );
        List<Map<String, Object>> rows = (List<Map<String, Object>>) response.getOrDefault("message", List.of());
        List<BillingInvoiceView> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            out.add(new BillingInvoiceView(
                asString(row.get("name")),
                asString(row.get("posting_date")),
                asString(row.get("due_date")),
                asString(row.get("status")),
                asString(row.get("currency")),
                asString(row.get("grand_total")),
                asString(row.get("outstanding_amount"))
            ));
        }
        return out;
    }

    public BillingInvoiceView findUnpaidInvoice(String patientId, String invoiceId) {
        return getUnpaidInvoices(patientId).stream()
            .filter(v -> Objects.equals(v.invoiceId(), invoiceId))
            .findFirst()
            .orElse(null);
    }

    public Map<String, Object> markSalesInvoicePaidFromMobile(
        String invoiceId,
        String amount,
        String modeOfPayment,
        String referenceId,
        String providerTxnId
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("invoice", invoiceId);
        payload.put("amount", amount);
        payload.put("mode_of_payment", modeOfPayment);
        payload.put("reference_id", referenceId);
        payload.put("provider_txn_id", providerTxnId);
        return frappeClient.postMethod(
            "prime.mobile_api.mark_sales_invoice_paid_from_mobile",
            payload
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
