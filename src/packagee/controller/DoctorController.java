package packagee.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import packagee.controller.interfaces.IDoctorController;
import packagee.model.DataStore;
import packagee.model.Doctor;
import packagee.model.IUser;
import packagee.model.Specialty;

public class DoctorController implements IController, IDoctorController {

    private final DataStore store;

    public DoctorController(DataStore store) {
        this.store = store;
    }

    public Response registerDoctor(String id, String username, String password, String confirmPassword,
                                   String firstname, String lastname, String licenseNumber, String office, String specialty) {
        if (!Validator.isValidId12Digits(id))
            return new Response(Response.BAD_REQUEST, "Invalid id (must be 12 digits, greater than 0)", null);
        long idL = Long.parseLong(id);
        if (existsUserWithId(idL))
            return new Response(Response.CONFLICT, "Id already exists", null);

        Response shared = validateSharedFields(username, password, confirmPassword, firstname, lastname,
                                               licenseNumber, office, specialty, -1L);
        if (shared != null) return shared;

        Doctor d = new Doctor(idL, username, firstname, lastname, password,
                              Specialty.valueOf(specialty), licenseNumber, office);
        if (!store.addDoctor(d))
            return new Response(Response.CONFLICT, "Could not register doctor", null);
        return new Response(Response.OK, "Doctor registered", serialize(d));
    }

    public Response updateDoctor(String id, String username, String password, String confirmPassword,
                                 String firstname, String lastname, String licenseNumber, String office, String specialty) {
        if (!Validator.isValidId12Digits(id))
            return new Response(Response.BAD_REQUEST, "Invalid id (must be 12 digits)", null);
        long idL = Long.parseLong(id);
        Doctor d = store.findDoctorById(idL);
        if (d == null)
            return new Response(Response.NOT_FOUND, "Doctor not found", null);

        Response shared = validateSharedFields(username, password, confirmPassword, firstname, lastname,
                                               licenseNumber, office, specialty, idL);
        if (shared != null) return shared;

        d.setUsername(username);
        d.setPassword(password);
        d.setFirstname(firstname);
        d.setLastname(lastname);
        d.setLicenceNumber(licenseNumber);
        d.setAssignedOffice(office);
        d.setSpecialty(Specialty.valueOf(specialty));
        store.notifyObservers("DOCTOR_UPDATED", store.serializeDoctor(d));
        return new Response(Response.OK, "Doctor updated", serialize(d));
    }

    public Response getDoctors() {
        List<Doctor> list = store.getDoctors();
        list.sort(Comparator.comparingLong(Doctor::getId));
        List<Map<String, Object>> data = new ArrayList<>(list.size());
        for (Doctor d : list) data.add(serialize(d));
        return new Response(Response.OK, "OK", data);
    }

    private Response validateSharedFields(String username, String password, String confirmPassword,
                                          String firstname, String lastname, String licenseNumber, String office,
                                          String specialty, long ignoreUserId) {
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
        if (!Validator.isValidLicense(licenseNumber))
            return new Response(Response.BAD_REQUEST, "Invalid license (expected L-XXXXXXXXXX MTL)", null);
        if (!Validator.isValidOffice(office))
            return new Response(Response.BAD_REQUEST, "Invalid office (expected O-XXX)", null);
        if (!Validator.isValidSpecialty(specialty))
            return new Response(Response.BAD_REQUEST, "Invalid specialty", null);
        return null;
    }

    private boolean existsUserWithId(long id) {
        for (IUser u : store.getUsers()) if (u.getId() == id) return true;
        return false;
    }

    static Map<String, Object> serialize(Doctor d) {
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
