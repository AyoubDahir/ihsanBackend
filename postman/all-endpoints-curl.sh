#!/usr/bin/env bash

# Alihsan Java Backend - Full cURL Collection
# Base URL
BASE_URL="https://api.alihsans.com"

# Variables (edit these before running)
MOBILE="252614440378"
OTP="123456"
PATIENT_ID="PID-00001"
PRACTITIONER_ID="DOC-0001"
INVOICE_ID="SINV-0001"
REFERENCE_ID="APPT-9cacc456-e7d3-4219-8377-81296620d6cc"
INVOICE_REFERENCE_ID="INV-REFERENCE-001"

echo "1) Health"
curl -sS "$BASE_URL/health"
echo -e "\n"

echo "2) Auth - Check Mobile"
curl -sS -X POST "$BASE_URL/api/mobile/auth/check-mobile" \
  -H "Content-Type: application/json" \
  -d "{
    \"mobile\": \"$MOBILE\"
  }"
echo -e "\n"

echo "3) Auth - Send OTP"
curl -sS -X POST "$BASE_URL/api/mobile/auth/send-otp" \
  -H "Content-Type: application/json" \
  -d "{
    \"mobile\": \"$MOBILE\"
  }"
echo -e "\n"

echo "4) Auth - Verify OTP"
curl -sS -X POST "$BASE_URL/api/mobile/auth/verify-otp" \
  -H "Content-Type: application/json" \
  -d "{
    \"mobile\": \"$MOBILE\",
    \"otp\": \"$OTP\"
  }"
echo -e "\n"

echo "5) Practitioners"
curl -sS "$BASE_URL/api/healthcare/practitioners"
echo -e "\n"

echo "6) Practitioners by Department"
curl -sS "$BASE_URL/api/healthcare/practitioners?department=General%20Medicine"
echo -e "\n"

echo "7) Create Appointment (existing patient)"
curl -sS -X POST "$BASE_URL/api/healthcare/appointments" \
  -H "Content-Type: application/json" \
  -d "{
    \"patientId\": \"$PATIENT_ID\",
    \"practitionerId\": \"$PRACTITIONER_ID\",
    \"appointmentDate\": \"2026-03-19\",
    \"appointmentTime\": \"10:00:00\",
    \"department\": \"General Medicine\",
    \"amount\": 5.00,
    \"phoneNumber\": \"$MOBILE\",
    \"paymentType\": \"EVC\"
  }"
echo -e "\n"

echo "8) Create Appointment (self-registration path)"
curl -sS -X POST "$BASE_URL/api/healthcare/appointments" \
  -H "Content-Type: application/json" \
  -d "{
    \"firstName\": \"Ahmed\",
    \"lastName\": \"Ali\",
    \"fullName\": \"Ahmed Ali\",
    \"mobile\": \"$MOBILE\",
    \"sex\": \"Male\",
    \"age\": 30,
    \"ageType\": \"Year\",
    \"practitionerId\": \"$PRACTITIONER_ID\",
    \"appointmentDate\": \"2026-03-19\",
    \"appointmentTime\": \"11:00:00\",
    \"department\": \"General Medicine\",
    \"amount\": 5.00,
    \"phoneNumber\": \"$MOBILE\",
    \"paymentType\": \"EVC\"
  }"
echo -e "\n"

echo "9) Payment Verify by Reference"
curl -sS "$BASE_URL/api/payment/verify/$REFERENCE_ID"
echo -e "\n"

echo "10) Payment Webhook (approve appointment)"
curl -sS -X POST "$BASE_URL/api/payment/webhook" \
  -H "Content-Type: application/json" \
  -d "{
    \"referenceId\": \"$REFERENCE_ID\",
    \"status\": \"APPROVED\",
    \"providerTxnId\": \"WAAFI-TXN-APPT-001\"
  }"
echo -e "\n"

echo "11) Patient Appointments"
curl -sS "$BASE_URL/api/healthcare/appointments?patientId=$PATIENT_ID"
echo -e "\n"

echo "12) Patient Lab Reports"
curl -sS "$BASE_URL/api/healthcare/lab-reports?patientId=$PATIENT_ID"
echo -e "\n"

echo "13) Billing - List Unpaid Invoices"
curl -sS "$BASE_URL/api/billing/invoices?patientId=$PATIENT_ID"
echo -e "\n"

echo "14) Billing - Get One Invoice"
curl -sS "$BASE_URL/api/billing/invoices/$INVOICE_ID?patientId=$PATIENT_ID"
echo -e "\n"

echo "15) Billing - Start Invoice Payment"
curl -sS -X POST "$BASE_URL/api/billing/pay" \
  -H "Content-Type: application/json" \
  -d "{
    \"patientId\": \"$PATIENT_ID\",
    \"invoiceId\": \"$INVOICE_ID\",
    \"amount\": 5.00,
    \"phoneNumber\": \"$MOBILE\",
    \"paymentType\": \"EVC\"
  }"
echo -e "\n"

echo "16) Payment Webhook (approve invoice)"
curl -sS -X POST "$BASE_URL/api/payment/webhook" \
  -H "Content-Type: application/json" \
  -d "{
    \"referenceId\": \"$INVOICE_REFERENCE_ID\",
    \"status\": \"APPROVED\",
    \"providerTxnId\": \"WAAFI-TXN-INV-001\"
  }"
echo -e "\n"

echo "17) Billing - Invoice Payment Status"
curl -sS "$BASE_URL/api/billing/payments/$INVOICE_REFERENCE_ID"
echo -e "\n"

