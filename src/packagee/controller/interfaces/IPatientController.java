package packagee.controller.interfaces;

import packagee.controller.IController;
import packagee.controller.Response;

public interface IPatientController extends IController {

    Response registerPatient(String id, String username, String password, String confirmPassword,
                             String firstname, String lastname, String phone, String email, String birthdate);

    Response updatePatient(String id, String username, String password, String confirmPassword,
                           String firstname, String lastname, String phone, String email, String birthdate);

    Response getPatients();
}
