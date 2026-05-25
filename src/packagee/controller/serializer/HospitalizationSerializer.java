package packagee.controller.serializer;

import java.util.LinkedHashMap;
import java.util.Map;
import packagee.model.Hospitalization;

public final class HospitalizationSerializer {

    private HospitalizationSerializer() {}

    public static Map<String, Object> serialize(Hospitalization h) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", h.getId());
        m.put("patientId", h.getPatient() == null ? null : h.getPatient().getId());
        m.put("patientName", h.getPatient() == null ? null
                : h.getPatient().getFirstname() + " " + h.getPatient().getLastname());
        m.put("doctorId", h.getDoctor() == null ? null : h.getDoctor().getId());
        m.put("doctorName", h.getDoctor() == null ? null
                : h.getDoctor().getFirstname() + " " + h.getDoctor().getLastname());
        m.put("date", h.getDate() == null ? null : h.getDate().toString());
        m.put("reason", h.getReason());
        m.put("roomType", h.getRoomType() == null ? null : h.getRoomType().name());
        m.put("observations", h.getObservations());
        m.put("status", h.getStatus() == null ? null : h.getStatus().name());
        return m;
    }
}
