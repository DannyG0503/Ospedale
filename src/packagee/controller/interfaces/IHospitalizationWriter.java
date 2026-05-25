package packagee.controller.interfaces;

import packagee.controller.IController;
import packagee.controller.Response;

public interface IHospitalizationWriter extends IController {

    Response requestHospitalization(String patientId, String reason, String date);

    Response approveHospitalization(String hospitalizationId, String doctorId);

    Response denyHospitalization(String hospitalizationId, String doctorId);

    Response hospitalizeFromAppointment(String appointmentId, String doctorId, String reason);
}
