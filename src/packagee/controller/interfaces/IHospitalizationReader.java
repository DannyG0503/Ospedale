package packagee.controller.interfaces;

import packagee.controller.IController;
import packagee.controller.Response;

public interface IHospitalizationReader extends IController {

    Response getHospitalizations();
}
