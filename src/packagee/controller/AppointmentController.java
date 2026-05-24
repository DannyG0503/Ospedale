package packagee.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import packagee.controller.interfaces.IAppointmentController;
import packagee.model.Appointment;
import packagee.model.AppointmentStatus;
import packagee.model.DataStore;
import packagee.model.Doctor;
import packagee.model.Patient;
import packagee.model.Prescription;
import packagee.model.Specialty;

public class AppointmentController implements IController, IAppointmentController {

    private final DataStore store;

    public AppointmentController(DataStore store) {
        this.store = store;
    }

    public Response requestAppointment(String patientId, String doctorId, String specialty,
                                       String date, String time, String reason, boolean byDoctor) {
        if (!Validator.isValidId12Digits(patientId))
            return new Response(Response.BAD_REQUEST, "Invalid patient id", null);
        Patient patient = store.findPatientById(Long.parseLong(patientId));
        if (patient == null)
            return new Response(Response.NOT_FOUND, "Patient not found", null);
        if (!Validator.isValidDate(date))
            return new Response(Response.BAD_REQUEST, "Invalid date (YYYY-MM-DD)", null);
        if (!Validator.isValidTime(time))
            return new Response(Response.BAD_REQUEST, "Invalid time (hh:mm, minutes 00/15/30/45)", null);

        LocalDateTime when = LocalDate.parse(date).atTime(LocalTime.parse(time));

        Doctor doctor;
        Specialty spec;
        if (byDoctor) {
            if (!Validator.isValidId12Digits(doctorId))
                return new Response(Response.BAD_REQUEST, "Invalid doctor id", null);
            doctor = store.findDoctorById(Long.parseLong(doctorId));
            if (doctor == null)
                return new Response(Response.NOT_FOUND, "Doctor not found", null);
            if (!isDoctorAvailable(doctor, when, null))
                return new Response(Response.CONFLICT, "Doctor not available at that slot", null);
            spec = doctor.getSpecialty();
        } else {
            if (!Validator.isValidSpecialty(specialty))
                return new Response(Response.BAD_REQUEST, "Invalid specialty", null);
            spec = Specialty.valueOf(specialty);
            doctor = findAvailableDoctorBySpecialty(spec, when);
            if (doctor == null)
                return new Response(Response.CONFLICT, "No doctor available for that specialty at that slot", null);
        }

        String apptId = nextAppointmentId(patient.getId());
        Appointment appt = new Appointment(apptId, patient, doctor, spec, when, reason, true);
        store.addAppointment(appt);
        return new Response(Response.OK, "Appointment requested", serialize(appt));
    }

    public Response acceptAppointment(String appointmentId, String doctorId) {
        Appointment a = store.findAppointmentById(appointmentId);
        if (a == null) return new Response(Response.NOT_FOUND, "Appointment not found", null);
        Response check = checkDoctorOwns(a, doctorId);
        if (check != null) return check;
        if (a.getStatus() != AppointmentStatus.REQUESTED)
            return new Response(Response.CONFLICT, "Appointment is not in REQUESTED state", null);
        a.setStatus(AppointmentStatus.PENDING);
        store.notifyObservers("APPOINTMENT_UPDATED", store.serializeAppointment(a));
        return new Response(Response.OK, "Appointment accepted", serialize(a));
    }

    public Response completeAppointment(String appointmentId, String doctorId,
                                        String diagnosis, String observations,
                                        String recommendedTreatment, String followUp) {
        Appointment a = store.findAppointmentById(appointmentId);
        if (a == null) return new Response(Response.NOT_FOUND, "Appointment not found", null);
        Response check = checkDoctorOwns(a, doctorId);
        if (check != null) return check;
        if (a.getStatus() != AppointmentStatus.PENDING)
            return new Response(Response.CONFLICT, "Appointment is not in PENDING state", null);
        a.setDiagnosis(diagnosis);
        a.setObservations(observations);
        a.setRecommendedTreatment(recommendedTreatment);
        a.setFollowUp(followUp);
        a.setStatus(AppointmentStatus.COMPLETED);
        store.notifyObservers("APPOINTMENT_UPDATED", store.serializeAppointment(a));
        return new Response(Response.OK, "Appointment completed", serialize(a));
    }

    public Response cancelAppointment(String appointmentId, String patientId) {
        Appointment a = store.findAppointmentById(appointmentId);
        if (a == null) return new Response(Response.NOT_FOUND, "Appointment not found", null);
        if (!Validator.isValidId12Digits(patientId))
            return new Response(Response.BAD_REQUEST, "Invalid patient id", null);
        if (a.getPatient() == null || a.getPatient().getId() != Long.parseLong(patientId))
            return new Response(Response.BAD_REQUEST, "Patient does not match this appointment", null);
        if (a.getStatus() == AppointmentStatus.COMPLETED)
            return new Response(Response.CONFLICT, "Cannot cancel a completed appointment", null);
        a.setStatus(AppointmentStatus.CANCELED);
        store.notifyObservers("APPOINTMENT_UPDATED", store.serializeAppointment(a));
        return new Response(Response.OK, "Appointment canceled", serialize(a));
    }

    public Response rescheduleAppointment(String appointmentId, String doctorId, String newTime, String rescheduleReason) {
        Appointment a = store.findAppointmentById(appointmentId);
        if (a == null) return new Response(Response.NOT_FOUND, "Appointment not found", null);
        Response check = checkDoctorOwns(a, doctorId);
        if (check != null) return check;
        if (!Validator.isValidTime(newTime))
            return new Response(Response.BAD_REQUEST, "Invalid time (hh:mm, minutes 00/15/30/45)", null);
        LocalDateTime newDt = a.getDatetime().toLocalDate().atTime(LocalTime.parse(newTime));
        if (!isDoctorAvailable(a.getDoctor(), newDt, a))
            return new Response(Response.CONFLICT, "Doctor not available at the new slot", null);
        a.setDatetime(newDt);
        String prev = a.getReason() == null ? "" : a.getReason();
        a.setReason(prev + " | Rescheduled: " + (rescheduleReason == null ? "" : rescheduleReason));
        store.notifyObservers("APPOINTMENT_UPDATED", store.serializeAppointment(a));
        return new Response(Response.OK, "Appointment rescheduled", serialize(a));
    }

    public Response prescribeMedication(String appointmentId, String doctorId, String medicationName, String dose,
                                        String administrationRoute, String duration, String frequency, String additionalInfo) {
        Appointment a = store.findAppointmentById(appointmentId);
        if (a == null) return new Response(Response.NOT_FOUND, "Appointment not found", null);
        Response check = checkDoctorOwns(a, doctorId);
        if (check != null) return check;
        if (a.getStatus() != AppointmentStatus.PENDING)
            return new Response(Response.CONFLICT, "Prescriptions only allowed on PENDING appointments", null);
        if (medicationName == null || medicationName.isEmpty())
            return new Response(Response.BAD_REQUEST, "Medication name is required", null);
        double doseD;
        int durationI;
        int freqI;
        try {
            doseD = Double.parseDouble(dose);
            durationI = Integer.parseInt(duration);
            freqI = Integer.parseInt(frequency);
        } catch (NullPointerException | NumberFormatException e) {
            return new Response(Response.BAD_REQUEST, "Numeric dose, duration and frequency required", null);
        }
        Prescription p = new Prescription(a, medicationName, doseD, administrationRoute, durationI, additionalInfo, freqI);
        a.addPrescription(p);
        store.notifyObservers("APPOINTMENT_UPDATED", store.serializeAppointment(a));
        return new Response(Response.OK, "Prescription added", serializePrescription(p));
    }

    public Response getAppointmentsByPatient(String patientId) {
        if (!Validator.isValidId12Digits(patientId))
            return new Response(Response.BAD_REQUEST, "Invalid patient id", null);
        long pId = Long.parseLong(patientId);
        List<Appointment> filtered = new ArrayList<>();
        for (Appointment a : store.getAppointments()) {
            if (a.getPatient() != null && a.getPatient().getId() == pId) filtered.add(a);
        }
        filtered.sort(Comparator.comparing(Appointment::getDatetime).reversed());
        List<Map<String, Object>> data = new ArrayList<>(filtered.size());
        for (Appointment a : filtered) data.add(serialize(a));
        return new Response(Response.OK, "OK", data);
    }

    public Response getAppointmentsByDoctor(String doctorId, boolean pendingOnly) {
        if (!Validator.isValidId12Digits(doctorId))
            return new Response(Response.BAD_REQUEST, "Invalid doctor id", null);
        long dId = Long.parseLong(doctorId);
        List<Appointment> filtered = new ArrayList<>();
        for (Appointment a : store.getAppointments()) {
            if (a.getDoctor() != null && a.getDoctor().getId() == dId) {
                if (!pendingOnly || a.getStatus() == AppointmentStatus.PENDING) filtered.add(a);
            }
        }
        filtered.sort(Comparator.comparing(Appointment::getDatetime).reversed());
        List<Map<String, Object>> data = new ArrayList<>(filtered.size());
        for (Appointment a : filtered) data.add(serialize(a));
        return new Response(Response.OK, "OK", data);
    }

    // ----- helpers -----

    private Response checkDoctorOwns(Appointment a, String doctorId) {
        if (!Validator.isValidId12Digits(doctorId))
            return new Response(Response.BAD_REQUEST, "Invalid doctor id", null);
        if (a.getDoctor() == null || a.getDoctor().getId() != Long.parseLong(doctorId))
            return new Response(Response.BAD_REQUEST, "Doctor does not match this appointment", null);
        return null;
    }

    private boolean isDoctorAvailable(Doctor d, LocalDateTime when, Appointment ignore) {
        for (Appointment a : store.getAppointments()) {
            if (ignore != null && a == ignore) continue;
            if (a.getDoctor() == null || a.getDoctor().getId() != d.getId()) continue;
            AppointmentStatus s = a.getStatus();
            if (s != AppointmentStatus.REQUESTED && s != AppointmentStatus.PENDING) continue;
            if (when.equals(a.getDatetime())) return false;
        }
        return true;
    }

    private Doctor findAvailableDoctorBySpecialty(Specialty s, LocalDateTime when) {
        for (Doctor d : store.getDoctors()) {
            if (d.getSpecialty() == s && isDoctorAvailable(d, when, null)) return d;
        }
        return null;
    }

    private String nextAppointmentId(long patientId) {
        String prefix = "A-" + patientId + "-";
        int max = -1;
        for (Appointment a : store.getAppointments()) {
            String aid = a.getId();
            if (aid != null && aid.startsWith(prefix)) {
                try {
                    max = Math.max(max, Integer.parseInt(aid.substring(prefix.length())));
                } catch (NumberFormatException ignored) {}
            }
        }
        return String.format("%s%04d", prefix, max + 1);
    }

    static Map<String, Object> serialize(Appointment a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("patientId", a.getPatient() == null ? null : a.getPatient().getId());
        m.put("patientName", a.getPatient() == null ? null : a.getPatient().getFirstname() + " " + a.getPatient().getLastname());
        m.put("doctorId", a.getDoctor() == null ? null : a.getDoctor().getId());
        m.put("doctorName", a.getDoctor() == null ? null : a.getDoctor().getFirstname() + " " + a.getDoctor().getLastname());
        m.put("specialty", a.getSpecialty() == null ? null : a.getSpecialty().name());
        m.put("datetime", a.getDatetime() == null ? null : a.getDatetime().toString());
        m.put("reason", a.getReason());
        m.put("type", a.isType() ? "IN_PERSON" : "REMOTE");
        m.put("status", a.getStatus() == null ? null : a.getStatus().name());
        m.put("diagnosis", a.getDiagnosis());
        m.put("observations", a.getObservations());
        m.put("recommendedTreatment", a.getRecommendedTreatment());
        m.put("followUp", a.getFollowUp());
        List<Map<String, Object>> ps = new ArrayList<>();
        if (a.getPrescriptions() != null) {
            for (Prescription p : a.getPrescriptions()) ps.add(serializePrescription(p));
        }
        m.put("prescriptions", ps);
        return m;
    }

    static Map<String, Object> serializePrescription(Prescription p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("appointmentId", p.getAppointment() == null ? null : p.getAppointment().getId());
        m.put("medicationName", p.getMedicationName());
        m.put("dose", p.getDose());
        m.put("administrationRoute", p.getAdministrationRoute());
        m.put("treatmentDuration", p.getTreatmentDuration());
        m.put("frequency", p.getFrequency());
        m.put("additionalInstructions", p.getAdditionalInstructions());
        return m;
    }
}
