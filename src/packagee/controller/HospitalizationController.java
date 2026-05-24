package packagee.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import packagee.controller.interfaces.IHospitalizationController;
import packagee.model.Appointment;
import packagee.model.AppointmentStatus;
import packagee.model.DataStore;
import packagee.model.Doctor;
import packagee.model.Hospitalization;
import packagee.model.HospitalizationStatus;
import packagee.model.Patient;
import packagee.model.RoomType;

public class HospitalizationController implements IController, IHospitalizationController {

    private final DataStore store;

    public HospitalizationController(DataStore store) {
        this.store = store;
    }

    public Response requestHospitalization(String patientId, String reason, String date) {
        if (!Validator.isValidId12Digits(patientId))
            return new Response(Response.BAD_REQUEST, "Invalid patient id", null);
        Patient patient = store.findPatientById(Long.parseLong(patientId));
        if (patient == null)
            return new Response(Response.NOT_FOUND, "Patient not found", null);
        if (!Validator.isValidDate(date))
            return new Response(Response.BAD_REQUEST, "Invalid date (YYYY-MM-DD)", null);

        String hospId = nextHospitalizationId(patient.getId());
        Hospitalization h = new Hospitalization(hospId, patient, null, LocalDate.parse(date),
                                                reason, RoomType.STANDARD, "");
        store.addHospitalization(h);
        return new Response(Response.OK, "Hospitalization requested", serialize(h));
    }

    public Response approveHospitalization(String hospitalizationId, String doctorId) {
        Hospitalization h = store.findHospitalizationById(hospitalizationId);
        if (h == null) return new Response(Response.NOT_FOUND, "Hospitalization not found", null);
        if (!Validator.isValidId12Digits(doctorId))
            return new Response(Response.BAD_REQUEST, "Invalid doctor id", null);
        Doctor doctor = store.findDoctorById(Long.parseLong(doctorId));
        if (doctor == null) return new Response(Response.NOT_FOUND, "Doctor not found", null);
        if (h.getStatus() != HospitalizationStatus.REQUESTED)
            return new Response(Response.CONFLICT, "Hospitalization is not in REQUESTED state", null);

        h.setStatus(HospitalizationStatus.ONGOING);
        if (h.getDoctor() == null) {
            h.setDoctor(doctor);
            doctor.addHospitalization(h);
        }
        store.notifyObservers("HOSPITALIZATION_UPDATED", store.serializeHospitalization(h));
        return new Response(Response.OK, "Hospitalization approved", serialize(h));
    }

    public Response denyHospitalization(String hospitalizationId, String doctorId) {
        Hospitalization h = store.findHospitalizationById(hospitalizationId);
        if (h == null) return new Response(Response.NOT_FOUND, "Hospitalization not found", null);
        if (!Validator.isValidId12Digits(doctorId))
            return new Response(Response.BAD_REQUEST, "Invalid doctor id", null);
        if (store.findDoctorById(Long.parseLong(doctorId)) == null)
            return new Response(Response.NOT_FOUND, "Doctor not found", null);
        if (h.getStatus() != HospitalizationStatus.REQUESTED)
            return new Response(Response.CONFLICT, "Hospitalization is not in REQUESTED state", null);
        h.setStatus(HospitalizationStatus.CANCELED);
        store.notifyObservers("HOSPITALIZATION_UPDATED", store.serializeHospitalization(h));
        return new Response(Response.OK, "Hospitalization denied", serialize(h));
    }

    public Response hospitalizeFromAppointment(String appointmentId, String doctorId, String reason) {
        Appointment a = store.findAppointmentById(appointmentId);
        if (a == null) return new Response(Response.NOT_FOUND, "Appointment not found", null);
        if (!Validator.isValidId12Digits(doctorId))
            return new Response(Response.BAD_REQUEST, "Invalid doctor id", null);
        if (a.getDoctor() == null || a.getDoctor().getId() != Long.parseLong(doctorId))
            return new Response(Response.BAD_REQUEST, "Doctor does not match the appointment", null);
        if (a.getStatus() != AppointmentStatus.PENDING)
            return new Response(Response.CONFLICT, "Appointment must be PENDING", null);

        a.setStatus(AppointmentStatus.COMPLETED);
        store.notifyObservers("APPOINTMENT_UPDATED", store.serializeAppointment(a));
        String hospId = nextHospitalizationId(a.getPatient().getId());
        Hospitalization h = new Hospitalization(hospId, a.getPatient(), a.getDoctor(),
                                                LocalDate.now(), reason, RoomType.STANDARD, "",
                                                HospitalizationStatus.ONGOING);
        store.addHospitalization(h);
        return new Response(Response.OK, "Hospitalization created from appointment", serialize(h));
    }

    public Response getHospitalizations() {
        List<Hospitalization> list = store.getHospitalizations();
        List<Map<String, Object>> data = new ArrayList<>(list.size());
        for (Hospitalization h : list) data.add(serialize(h));
        return new Response(Response.OK, "OK", data);
    }

    private String nextHospitalizationId(long patientId) {
        String prefix = "H-" + patientId + "-";
        int max = -1;
        for (Hospitalization h : store.getHospitalizations()) {
            String hid = h.getId();
            if (hid != null && hid.startsWith(prefix)) {
                try {
                    max = Math.max(max, Integer.parseInt(hid.substring(prefix.length())));
                } catch (NumberFormatException ignored) {}
            }
        }
        return String.format("%s%04d", prefix, max + 1);
    }

    static Map<String, Object> serialize(Hospitalization h) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", h.getId());
        m.put("patientId", h.getPatient() == null ? null : h.getPatient().getId());
        m.put("patientName", h.getPatient() == null ? null : h.getPatient().getFirstname() + " " + h.getPatient().getLastname());
        m.put("doctorId", h.getDoctor() == null ? null : h.getDoctor().getId());
        m.put("doctorName", h.getDoctor() == null ? null : h.getDoctor().getFirstname() + " " + h.getDoctor().getLastname());
        m.put("date", h.getDate() == null ? null : h.getDate().toString());
        m.put("reason", h.getReason());
        m.put("roomType", h.getRoomType() == null ? null : h.getRoomType().name());
        m.put("observations", h.getObservations());
        m.put("status", h.getStatus() == null ? null : h.getStatus().name());
        return m;
    }
}
