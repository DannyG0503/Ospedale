package packagee.controller.serializer;

import java.util.LinkedHashMap;
import java.util.Map;
import packagee.model.Prescription;

public final class PrescriptionSerializer {

    private PrescriptionSerializer() {}

    public static Map<String, Object> serialize(Prescription p) {
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
