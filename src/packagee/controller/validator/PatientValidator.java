package packagee.controller.validator;

import packagee.controller.Response;
import packagee.controller.Validator;
import packagee.model.IDataStore;
import packagee.model.IUser;

public final class PatientValidator {

    private final IDataStore store;

    public PatientValidator(IDataStore store) {
        this.store = store;
    }

    public Response validateForRegister(String id, String username, String password, String confirmPassword,
                                        String firstname, String lastname, String phone, String email,
                                        String birthdate) {
        if (!Validator.isValidId12Digits(id))
            return new Response(Response.BAD_REQUEST, "Invalid id (must be 12 digits, greater than 0)", null);
        long idL = Long.parseLong(id);
        if (existsUserWithId(idL))
            return new Response(Response.CONFLICT, "Id already exists", null);
        return validateSharedFields(username, password, confirmPassword, firstname, lastname,
                                    phone, email, birthdate, -1L);
    }

    public Response validateForUpdate(String username, String password, String confirmPassword,
                                      String firstname, String lastname, String phone, String email,
                                      String birthdate, long ignoreUserId) {
        return validateSharedFields(username, password, confirmPassword, firstname, lastname,
                                    phone, email, birthdate, ignoreUserId);
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
}
