package packagee.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import packagee.controller.interfaces.IPatientController;
import packagee.model.DataStore;
import packagee.model.IUser;
import packagee.model.Patient;

public class PatientController implements IController, IPatientController {

    private final DataStore store;

    public PatientController(DataStore store) {
        this.store = store;
    }

    public Response registerPatient(String id, String username, String password, String confirmPassword,
                                    String firstname, String lastname, String phone, String email, String birthdate) {
        if (!Validator.isValidId12Digits(id))
            return new Response(Response.BAD_REQUEST, "Invalid id (must be 12 digits, greater than 0)", null);
        long idL = Long.parseLong(id);
        if (existsUserWithId(idL))
            return new Response(Response.CONFLICT, "Id already exists", null);

        Response shared = validateSharedFields(username, password, confirmPassword, firstname, lastname,
                                               phone, email, birthdate, -1L);
        if (shared != null) return shared;

        Patient p = new Patient(idL, username, firstname, lastname, password,
                                email, LocalDate.parse(birthdate), false, Long.parseLong(phone), "");
        if (!store.addPatient(p))
            return new Response(Response.CONFLICT, "Could not register patient", null);
        return new Response(Response.OK, "Patient registered", serialize(p));
    }

    public Response updatePatient(String id, String username, String password, String confirmPassword,
                                  String firstname, String lastname, String phone, String email, String birthdate) {
        if (!Validator.isValidId12Digits(id))
            return new Response(Response.BAD_REQUEST, "Invalid id (must be 12 digits)", null);
        long idL = Long.parseLong(id);
        Patient p = store.findPatientById(idL);
        if (p == null)
            return new Response(Response.NOT_FOUND, "Patient not found", null);

        Response shared = validateSharedFields(username, password, confirmPassword, firstname, lastname,
                                               phone, email, birthdate, idL);
        if (shared != null) return shared;

        p.setUsername(username);
        p.setPassword(password);
        p.setFirstname(firstname);
        p.setLastname(lastname);
        p.setPhone(Long.parseLong(phone));
        p.setEmail(email);
        p.setBirthdate(LocalDate.parse(birthdate));
        store.notifyObservers("PATIENT_UPDATED", store.serializePatient(p));
        return new Response(Response.OK, "Patient updated", serialize(p));
    }

    public Response getPatients() {
        List<Patient> list = store.getPatients();
        list.sort(Comparator.comparingLong(Patient::getId));
        List<Map<String, Object>> data = new ArrayList<>(list.size());
        for (Patient p : list) data.add(serialize(p));
        return new Response(Response.OK, "OK", data);
    }

    private Response validateSharedFields(String username, String password, String confirmPassword,
                                          String firstname, String lastname, String phone, String email,
                                          String birthdate, long ignoreUserId) {
        if (username == null || username.isEmpty())
            return new Response(Response.BAD_REQUEST, "Username is required", null);
        IUser other = store.findUserByUsername(username);
        if (other != null && other.getId() != ignoreUserId)
            return new Response(Response.CONFLICT, "Username already in use", null);
        if (password == null || password.isEmpty())
            return new Response(Response.BAD_REQUEST, "Password is required", null);
        if (!password.equals(confirmPassword))
            return new Response(Response.BAD_REQUEST, "Passwords do not match", null);
        if (firstname == null || firstname.isEmpty())
            return new Response(Response.BAD_REQUEST, "Firstname is required", null);
        if (lastname == null || lastname.isEmpty())
            return new Response(Response.BAD_REQUEST, "Lastname is required", null);
        if (!Validator.isValidPhone(phone))
            return new Response(Response.BAD_REQUEST, "Invalid phone (must be exactly 10 digits)", null);
        if (!Validator.isValidEmail(email))
            return new Response(Response.BAD_REQUEST, "Invalid email (expected XXXXX@XXXXX.com)", null);
        if (!Validator.isValidDate(birthdate))
            return new Response(Response.BAD_REQUEST, "Invalid birthdate (expected YYYY-MM-DD)", null);
        return null;
    }

    private boolean existsUserWithId(long id) {
        for (IUser u : store.getUsers()) if (u.getId() == id) return true;
        return false;
    }

    static Map<String, Object> serialize(Patient p) {
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
