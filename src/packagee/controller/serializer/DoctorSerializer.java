package packagee.controller.serializer;

import java.util.LinkedHashMap;
import java.util.Map;
import packagee.model.Doctor;

public final class DoctorSerializer {

    private DoctorSerializer() {}

    public static Map<String, Object> serialize(Doctor d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("username", d.getUsername());
        m.put("firstname", d.getFirstname());
        m.put("lastname", d.getLastname());
        m.put("specialty", d.getSpecialty() == null ? null : d.getSpecialty().name());
        m.put("licenceNumber", d.getLicenceNumber());
        m.put("assignedOffice", d.getAssignedOffice());
        return m;
    }
}
