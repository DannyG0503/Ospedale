package packagee.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import packagee.controller.interfaces.IAuthController;
import packagee.model.Administrator;
import packagee.model.DataStore;
import packagee.model.Doctor;
import packagee.model.IUser;
import packagee.model.Patient;

public class AuthController implements IController, IAuthController {

    private final DataStore store;

    public AuthController(DataStore store) {
        this.store = store;
    }

    public Response login(String username, String password) {
        if (username == null || username.isEmpty()) {
            return new Response(Response.BAD_REQUEST, "Username is required", null);
        }
        IUser user = store.findUserByUsername(username);
        if (user == null) {
            return new Response(Response.NOT_FOUND, "User not found", null);
        }
        if (password == null || !password.equals(user.getPassword())) {
            return new Response(Response.BAD_REQUEST, "Incorrect password", null);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("firstname", user.getFirstname());
        data.put("lastname", user.getLastname());
        data.put("role", roleOf(user));
        return new Response(Response.OK, "Login successful", data);
    }

    private static String roleOf(IUser u) {
        if (u instanceof Administrator) return "ADMIN";
        if (u instanceof Doctor) return "DOCTOR";
        if (u instanceof Patient) return "PATIENT";
        return "UNKNOWN";
    }
}
