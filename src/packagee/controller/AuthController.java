package packagee.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import packagee.controller.interfaces.IAuthController;
import packagee.model.IDataStore;
import packagee.model.IUser;
import packagee.model.Role;

public class AuthController implements IController, IAuthController {

    private final IDataStore store;

    public AuthController(IDataStore store) {
        this.store = store;
    }

    @Override
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
        Role role = user.getRole();
        data.put("role", role == null ? "UNKNOWN" : role.name());
        return new Response(Response.OK, "Login successful", data);
    }
}
