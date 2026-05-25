package packagee.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import packagee.controller.interfaces.IDoctorController;
import packagee.controller.serializer.DoctorSerializer;
import packagee.controller.validator.DoctorValidator;
import packagee.model.Doctor;
import packagee.model.IDataStore;
import packagee.model.Specialty;

public class DoctorController implements IController, IDoctorController {

    private final IDataStore store;
    private final DoctorValidator validator;

    public DoctorController(IDataStore store) {
        this.store = store;
        this.validator = new DoctorValidator(store);
    }

    @Override
    public Response registerDoctor(String id, String username, String password, String confirmPassword,
                                   String firstname, String lastname, String licenseNumber, String office, String specialty) {
        Response v = validator.validateForRegister(id, username, password, confirmPassword, firstname, lastname,
                                                   licenseNumber, office, specialty);
        if (v != null) return v;

        long idL = Long.parseLong(id);
        Doctor d = new Doctor(idL, username, firstname, lastname, password,
                              Specialty.valueOf(specialty), licenseNumber, office);
        if (!store.addDoctor(d))
            return new Response(Response.CONFLICT, "Could not register doctor", null);
        return new Response(Response.OK, "Doctor registered", DoctorSerializer.serialize(d));
    }

    @Override
    public Response updateDoctor(String id, String username, String password, String confirmPassword,
                                 String firstname, String lastname, String licenseNumber, String office, String specialty) {
        if (!Validator.isValidId12Digits(id))
            return new Response(Response.BAD_REQUEST, "Invalid id (must be 12 digits)", null);
        long idL = Long.parseLong(id);
        Doctor d = store.findDoctorById(idL);
        if (d == null)
            return new Response(Response.NOT_FOUND, "Doctor not found", null);

        Response v = validator.validateForUpdate(username, password, confirmPassword, firstname, lastname,
                                                 licenseNumber, office, specialty, idL);
        if (v != null) return v;

        d.setUsername(username);
        d.setPassword(password);
        d.setFirstname(firstname);
        d.setLastname(lastname);
        d.setLicenceNumber(licenseNumber);
        d.setAssignedOffice(office);
        d.setSpecialty(Specialty.valueOf(specialty));
        store.notifyObservers("DOCTOR_UPDATED", DoctorSerializer.serialize(d));
        return new Response(Response.OK, "Doctor updated", DoctorSerializer.serialize(d));
    }

    @Override
    public Response getDoctors() {
        List<Doctor> list = store.getDoctors();
        list.sort(Comparator.comparingLong(Doctor::getId));
        List<Map<String, Object>> data = new ArrayList<>(list.size());
        for (Doctor d : list) data.add(DoctorSerializer.serialize(d));
        return new Response(Response.OK, "OK", data);
    }
}
