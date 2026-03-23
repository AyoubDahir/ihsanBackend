package com.alihsan.backend.service;

import com.alihsan.backend.dto.BillingInvoiceView;
import com.alihsan.backend.dto.LabReportView;
import com.alihsan.backend.dto.PatientAppointmentView;
import com.alihsan.backend.dto.PractitionerSlotView;
import com.alihsan.backend.dto.PractitionerView;
import com.alihsan.backend.dto.QueueStatusView;
import com.alihsan.backend.integration.FrappeClient;
import com.alihsan.backend.util.MobileNumberUtil;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PrimeWorkflowService {
    private static final Logger log = LoggerFactory.getLogger(PrimeWorkflowService.class);

    private final FrappeClient frappeClient;
    private final SmsService smsService;
    private final Set<String> calledSmsSent = ConcurrentHashMap.newKeySet();

    public PrimeWorkflowService(FrappeClient frappeClient, SmsService smsService) {
        this.frappeClient = frappeClient;
        this.smsService = smsService;
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
        return createQueFromMobile(patientId, practitionerId, appointmentDate, appointmentTime, department, referenceId, null, null);
    }

    public Map<String, Object> createQueFromMobile(
        String patientId,
        String practitionerId,
        String appointmentDate,
        String appointmentTime,
        String department,
        String referenceId,
        BigDecimal paidAmount,
        String modeOfPayment
    ) {
        Map<String, Object> params = new HashMap<>();
        params.put("patient", patientId);
        params.put("practitioner", practitionerId);
        params.put("appointment_date", appointmentDate);
        params.put("appointment_time", appointmentTime);
        params.put("department", department);
        params.put("reference_id", referenceId);
        if (paidAmount != null) params.put("paid_amount", paidAmount.toPlainString());
        if (modeOfPayment != null) params.put("mode_of_payment", modeOfPayment);
        return frappeClient.postMethod("prime.api.mobile_api.create_que_from_mobile", params);
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> findPatientByMobile(String mobile) {
        String normalizedMobile = MobileNumberUtil.normalize(mobile);
        if (normalizedMobile == null || normalizedMobile.isEmpty()) {
            return null;
        }
        Map<String, Object> response = frappeClient.getResource(
            "Patient",
            Map.of(
                "fields", "[\"name\",\"patient_name\",\"mobile\"]",
                "filters", buildMobileLookupFilter(normalizedMobile),
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
        String normalizedMobile = MobileNumberUtil.normalize(mobile);
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
            "prime.api.mobile_api.register_patient_from_mobile",
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
                "fields", "[\"name\",\"practitioner_name\",\"department\",\"op_consulting_charge\"]",
                "filters", filters,
                "order_by", "modified desc",
                "limit_page_length", "200"
            )
        );
        List<Map<String, Object>> rows = (List<Map<String, Object>>) response.getOrDefault("data", List.of());
        List<PractitionerView> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            out.add(new PractitionerView(
                asString(row.get("name")),
                asString(row.get("practitioner_name")),
                asString(row.get("department")),
                asString(row.get("op_consulting_charge")),
                true
            ));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    public List<PractitionerSlotView> getPractitionerSlots(String practitionerId, String date) {
        Map<String, Object> response = frappeClient.postMethod(
            "healthcare.healthcare.doctype.patient_appointment.patient_appointment.get_availability_data",
            Map.of(
                "practitioner", practitionerId,
                "date", date,
                "appointment", "{\"doctype\":\"Patient Appointment\",\"__islocal\":1,\"patient\":\"\",\"invoiced\":0}"
            )
        );
        Map<String, Object> message = (Map<String, Object>) response.getOrDefault("message", Map.of());
        List<Map<String, Object>> slotDetails = (List<Map<String, Object>>) message.getOrDefault("slot_details", List.of());
        List<PractitionerSlotView> slots = new ArrayList<>();
        for (Map<String, Object> slotDetail : slotDetails) {
            String slotName = asString(slotDetail.get("slot_name"));
            String serviceUnit = asString(slotDetail.get("service_unit"));
            boolean teleConf = asBoolean(slotDetail.get("tele_conf"));
            boolean allowOverlap = asBoolean(slotDetail.get("allow_overlap"));
            Integer capacity = asInteger(slotDetail.get("service_unit_capacity"));

            List<Map<String, Object>> bookedAppointments = (List<Map<String, Object>>) slotDetail.getOrDefault("appointments", List.of());
            List<Map<String, Object>> availSlots = (List<Map<String, Object>>) slotDetail.getOrDefault("avail_slot", List.of());
            for (Map<String, Object> availSlot : availSlots) {
                String fromTime = asString(availSlot.get("from_time"));
                String toTime = asString(availSlot.get("to_time"));
                int bookedCount = countBookingsAtTime(bookedAppointments, fromTime);
                boolean available = bookedCount == 0;
                Integer remainingCapacity = null;
                if (allowOverlap && capacity != null && capacity > 0) {
                    remainingCapacity = Math.max(capacity - bookedCount, 0);
                    available = remainingCapacity > 0;
                }
                slots.add(new PractitionerSlotView(
                    slotName,
                    serviceUnit,
                    date,
                    fromTime,
                    toTime,
                    available,
                    bookedCount,
                    capacity,
                    remainingCapacity,
                    teleConf,
                    allowOverlap
                ));
            }
        }
        return slots;
    }

    @SuppressWarnings("unchecked")
    public List<BillingInvoiceView> getUnpaidInvoices(String patientId) {
        Map<String, Object> response = frappeClient.postMethod(
            "prime.api.mobile_api.get_unpaid_sales_invoices_for_mobile",
            Map.of("patient", patientId, "limit", 100)
        );
        List<Map<String, Object>> rows = (List<Map<String, Object>>) response.getOrDefault("message", List.of());
        List<BillingInvoiceView> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            List<Map<String, Object>> items = new ArrayList<>();
            Object rawItems = row.get("items");
            if (rawItems instanceof List<?> rawList) {
                for (Object item : rawList) {
                    if (item instanceof Map<?, ?> itemMap) {
                        Map<String, Object> mapped = new java.util.LinkedHashMap<>();
                        itemMap.forEach((k, v) -> mapped.put(String.valueOf(k), v));
                        items.add(mapped);
                    }
                }
            }
            out.add(new BillingInvoiceView(
                asString(row.get("name")),
                asString(row.get("posting_date")),
                asString(row.get("due_date")),
                asString(row.get("status")),
                asString(row.get("currency")),
                asString(row.get("grand_total")),
                asString(row.get("outstanding_amount")),
                items
            ));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    public List<BillingInvoiceView> getPaidInvoices(String patientId) {
        Map<String, Object> response = frappeClient.postMethod(
            "prime.api.mobile_api.get_paid_sales_invoices_for_mobile",
            Map.of("patient", patientId, "limit", 100)
        );
        List<Map<String, Object>> rows = (List<Map<String, Object>>) response.getOrDefault("message", List.of());
        List<BillingInvoiceView> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            List<Map<String, Object>> items = new ArrayList<>();
            Object rawItems = row.get("items");
            if (rawItems instanceof List<?> rawList) {
                for (Object item : rawList) {
                    if (item instanceof Map<?, ?> itemMap) {
                        Map<String, Object> mapped = new java.util.LinkedHashMap<>();
                        itemMap.forEach((k, v) -> mapped.put(String.valueOf(k), v));
                        items.add(mapped);
                    }
                }
            }
            out.add(new BillingInvoiceView(
                asString(row.get("name")),
                asString(row.get("posting_date")),
                asString(row.get("due_date")),
                asString(row.get("status")),
                asString(row.get("currency")),
                asString(row.get("grand_total")),
                asString(row.get("outstanding_amount")),
                items
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
            "prime.api.mobile_api.mark_sales_invoice_paid_from_mobile",
            payload
        );
    }

    @SuppressWarnings("unchecked")
    public List<PatientAppointmentView> getAppointments(String patientId) {
        Map<String, Object> response = frappeClient.getResource(
            "Que",
            Map.of(
                "fields", "[\"name\",\"patient\",\"practitioner\",\"practitioner_name\",\"date\",\"time\",\"status\",\"department\"]",
                "filters", "[[\"patient\",\"=\",\"" + patientId + "\"]]",
                "order_by", "date desc"
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
                asString(row.get("date")),
                asString(row.get("time")),
                asString(row.get("status")),
                asString(row.get("department"))
            ));
        }
        return mapped;
    }

    @SuppressWarnings("unchecked")
    public QueueStatusView getQueueStatus(String queId) {
        Map<String, Object> response = frappeClient.postMethod(
            "prime.api.queue_display_api.get_queue_status",
            Map.of("que_name", queId)
        );
        Map<String, Object> msg = (Map<String, Object>) response.getOrDefault("message", Map.of());
        boolean found = Boolean.TRUE.equals(msg.get("found"));
        if (!found) return new QueueStatusView(false, null, null, null, null, null, null, null, null);

        String status = asString(msg.get("status"));
        String patientName = asString(msg.get("patient_name"));
        if ("Called".equals(status) && calledSmsSent.add(queId)) {
            trySendCalledSms(queId, patientName);
        }

        return new QueueStatusView(
            true,
            asString(msg.get("que")),
            msg.get("token_no") instanceof Number n ? n.intValue() : null,
            asString(msg.get("patient_name")),
            asString(msg.get("practitioner_name")),
            asString(msg.get("department")),
            status,
            asString(msg.get("que_steps")),
            msg.get("patients_ahead") instanceof Number n ? n.intValue() : null
        );
    }

    @SuppressWarnings("unchecked")
    private void trySendCalledSms(String queId, String patientName) {
        try {
            Map<String, Object> queDoc = frappeClient.getResource("Que/" + queId, Map.of());
            Map<String, Object> data = (Map<String, Object>) queDoc.get("data");
            String patientId = asString(data != null ? data.get("patient") : null);
            if (patientId == null || patientId.isBlank()) return;

            Map<String, Object> patientDoc = frappeClient.getResource("Patient/" + patientId, Map.of());
            Map<String, Object> patData = (Map<String, Object>) patientDoc.get("data");
            String mobile = asString(patData != null ? patData.get("mobile") : null);
            if (mobile == null || mobile.isBlank()) return;

            smsService.sendCalledSms(mobile, patientName);
            log.info("Called SMS sent for que={} patient={}", queId, patientId);
        } catch (Exception e) {
            log.warn("Failed to send called SMS for que={}: {}", queId, e.getMessage());
            calledSmsSent.remove(queId); // allow retry on next poll
        }
    }

    @SuppressWarnings("unchecked")
    public List<LabReportView> getLabReports(String patientId) {
        Map<String, Object> response = frappeClient.postMethod(
            "prime.api.mobile_api.get_lab_reports_for_mobile",
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

    private boolean asBoolean(Object val) {
        String v = asString(val);
        return "1".equals(v) || "true".equalsIgnoreCase(v);
    }

    private Integer asInteger(Object val) {
        if (val == null) {
            return null;
        }
        try {
            return (int) Math.round(Double.parseDouble(String.valueOf(val)));
        } catch (Exception ex) {
            return null;
        }
    }

    private int countBookingsAtTime(List<Map<String, Object>> appointments, String fromTime) {
        if (fromTime == null || appointments == null || appointments.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Map<String, Object> appt : appointments) {
            String apptTime = asString(appt.get("appointment_time"));
            if (apptTime == null) {
                continue;
            }
            if (apptTime.equals(fromTime)) {
                count++;
            }
        }
        return count;
    }

    private String buildMobileLookupFilter(String normalizedMobile) {
        List<String> candidates = new ArrayList<>();
        candidates.add(normalizedMobile);
        if (normalizedMobile.startsWith("252") && normalizedMobile.length() > 3) {
            candidates.add("0" + normalizedMobile.substring(3));
        }
        candidates.add("+" + normalizedMobile);

        StringJoiner joiner = new StringJoiner("\",\"", "[\"", "\"]");
        for (String candidate : candidates) {
            joiner.add(candidate);
        }
        return "[[\"mobile\",\"in\"," + joiner + "]]";
    }
}
