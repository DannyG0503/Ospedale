package packagee.controller.interfaces;

import packagee.controller.IController;
import packagee.controller.Response;

public interface IDoctorWriter extends IController {

    Response registerDoctor(String id, String username, String password, String confirmPassword,
                            String firstname, String lastname, String licenseNumber, String office, String specialty);

    Response updateDoctor(String id, String username, String password, String confirmPassword,
                          String firstname, String lastname, String licenseNumber, String office, String specialty);
}
