package packagee.controller.interfaces;

import packagee.controller.IController;
import packagee.controller.Response;

public interface IPatientReader extends IController {

    Response getPatients();
}
