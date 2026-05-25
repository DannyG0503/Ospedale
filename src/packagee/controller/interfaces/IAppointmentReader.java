package packagee.controller.interfaces;

import packagee.controller.IController;
import packagee.controller.Response;

public interface IAppointmentReader extends IController {

    Response getAppointmentsByPatient(String patientId);

    Response getAppointmentsByDoctor(String doctorId, boolean pendingOnly);
}
