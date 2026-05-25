package packagee.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import packagee.controller.interfaces.IHospitalizationController;
import packagee.controller.serializer.AppointmentSerializer;
import packagee.controller.serializer.HospitalizationSerializer;
import packagee.model.Appointment;
import packagee.model.AppointmentStatus;
import packagee.model.Doctor;
import packagee.model.Hospitalization;
import packagee.model.HospitalizationStatus;
import packagee.model.IDataStore;
import packagee.model.Patient;
import packagee.model.RoomType;

public class HospitalizationController implements IController, IHospitalizationController {

    private final IDataStore store;

    public HospitalizationController(IDataStore store) {
        this.store = store;
    }

    @Override
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
        return new Response(Response.OK, "Hospitalization requested", HospitalizationSerializer.serialize(h));
    }

    @Override
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
        store.notifyObservers("HOSPITALIZATION_UPDATED", HospitalizationSerializer.serialize(h));
        return new Response(Response.OK, "Hospitalization approved", HospitalizationSerializer.serialize(h));
    }

    @Override
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
        store.notifyObservers("HOSPITALIZATION_UPDATED", HospitalizationSerializer.serialize(h));
        return new Response(Response.OK, "Hospitalization denied", HospitalizationSerializer.serialize(h));
    }

    @Override
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
        store.notifyObservers("APPOINTMENT_UPDATED", AppointmentSerializer.serialize(a));
        String hospId = nextHospitalizationId(a.getPatient().getId());
        Hospitalization h = new Hospitalization(hospId, a.getPatient(), a.getDoctor(),
                                                LocalDate.now(), reason, RoomType.STANDARD, "",
                                                HospitalizationStatus.ONGOING);
        store.addHospitalization(h);
        return new Response(Response.OK, "Hospitalization created from appointment", HospitalizationSerializer.serialize(h));
    }

    @Override
    public Response getHospitalizations() {
        List<Hospitalization> list = store.getHospitalizations();
        List<Map<String, Object>> data = new ArrayList<>(list.size());
        for (Hospitalization h : list) data.add(HospitalizationSerializer.serialize(h));
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
}
