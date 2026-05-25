package packagee.controller.serializer;

import java.util.LinkedHashMap;
import java.util.Map;
import packagee.model.Patient;

public final class PatientSerializer {

    private PatientSerializer() {}

    public static Map<String, Object> serialize(Patient p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("username", p.getUsername());
        m.put("firstname", p.getFirstname());
        m.put("lastname", p.getLastname());
        m.put("email", p.getEmail());
        m.put("birthdate", p.getBirthdate() == null ? null : p.getBirthdate().toString());
        m.put("gender", p.isGender());
        m.put("phone", p.getPhone());
        m.put("address", p.getAddress());
        return m;
    }
}
