package packagee.controller.validator;

import packagee.controller.Response;
import packagee.controller.Validator;
import packagee.model.IDataStore;
import packagee.model.IUser;

public final class DoctorValidator {

    private final IDataStore store;

    public DoctorValidator(IDataStore store) {
        this.store = store;
    }

    public Response validateForRegister(String id, String username, String password, String confirmPassword,
                                        String firstname, String lastname, String licenseNumber, String office,
                                        String specialty) {
        if (!Validator.isValidId12Digits(id))
            return new Response(Response.BAD_REQUEST, "Invalid id (must be 12 digits, greater than 0)", null);
        long idL = Long.parseLong(id);
        if (existsUserWithId(idL))
            return new Response(Response.CONFLICT, "Id already exists", null);
        return validateSharedFields(username, password, confirmPassword, firstname, lastname,
                                    licenseNumber, office, specialty, -1L);
    }

    public Response validateForUpdate(String username, String password, String confirmPassword,
                                      String firstname, String lastname, String licenseNumber, String office,
                                      String specialty, long ignoreUserId) {
        return validateSharedFields(username, password, confirmPassword, firstname, lastname,
                                    licenseNumber, office, specialty, ignoreUserId);
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
}
