package packagee.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import packagee.controller.interfaces.IPatientController;
import packagee.controller.serializer.PatientSerializer;
import packagee.controller.validator.PatientValidator;
import packagee.model.IDataStore;
import packagee.model.Patient;

public class PatientController implements IController, IPatientController {

    private final IDataStore store;
    private final PatientValidator validator;

    public PatientController(IDataStore store) {
        this.store = store;
        this.validator = new PatientValidator(store);
    }

    @Override
    public Response registerPatient(String id, String username, String password, String confirmPassword,
                                    String firstname, String lastname, String phone, String email, String birthdate) {
        Response v = validator.validateForRegister(id, username, password, confirmPassword,
                                                   firstname, lastname, phone, email, birthdate);
        if (v != null) return v;

        long idL = Long.parseLong(id);
        Patient p = new Patient(idL, username, firstname, lastname, password,
                                email, LocalDate.parse(birthdate), false, Long.parseLong(phone), "");
        if (!store.addPatient(p))
            return new Response(Response.CONFLICT, "Could not register patient", null);
        return new Response(Response.OK, "Patient registered", PatientSerializer.serialize(p));
    }

    @Override
    public Response updatePatient(String id, String username, String password, String confirmPassword,
                                  String firstname, String lastname, String phone, String email, String birthdate) {
        if (!Validator.isValidId12Digits(id))
            return new Response(Response.BAD_REQUEST, "Invalid id (must be 12 digits)", null);
        long idL = Long.parseLong(id);
        Patient p = store.findPatientById(idL);
        if (p == null)
            return new Response(Response.NOT_FOUND, "Patient not found", null);

        Response v = validator.validateForUpdate(username, password, confirmPassword, firstname, lastname,
                                                 phone, email, birthdate, idL);
        if (v != null) return v;

        p.setUsername(username);
        p.setPassword(password);
        p.setFirstname(firstname);
        p.setLastname(lastname);
        p.setPhone(Long.parseLong(phone));
        p.setEmail(email);
        p.setBirthdate(LocalDate.parse(birthdate));
        store.notifyObservers("PATIENT_UPDATED", PatientSerializer.serialize(p));
        return new Response(Response.OK, "Patient updated", PatientSerializer.serialize(p));
    }

    @Override
    public Response getPatients() {
        List<Patient> list = store.getPatients();
        list.sort(Comparator.comparingLong(Patient::getId));
        List<Map<String, Object>> data = new ArrayList<>(list.size());
        for (Patient p : list) data.add(PatientSerializer.serialize(p));
        return new Response(Response.OK, "OK", data);
    }
}
