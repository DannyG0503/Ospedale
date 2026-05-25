package packagee.controller.serializer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import packagee.model.Appointment;
import packagee.model.Prescription;

public final class AppointmentSerializer {

    private AppointmentSerializer() {}

    public static Map<String, Object> serialize(Appointment a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("patientId", a.getPatient() == null ? null : a.getPatient().getId());
        m.put("patientName", a.getPatient() == null ? null
                : a.getPatient().getFirstname() + " " + a.getPatient().getLastname());
        m.put("doctorId", a.getDoctor() == null ? null : a.getDoctor().getId());
        m.put("doctorName", a.getDoctor() == null ? null
                : a.getDoctor().getFirstname() + " " + a.getDoctor().getLastname());
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
            for (Prescription p : a.getPrescriptions()) ps.add(PrescriptionSerializer.serialize(p));
        }
        m.put("prescriptions", ps);
        return m;
    }
}
