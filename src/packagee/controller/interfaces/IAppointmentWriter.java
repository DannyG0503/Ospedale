package packagee.controller.interfaces;

import packagee.controller.IController;
import packagee.controller.Response;

public interface IAppointmentWriter extends IController {

    Response requestAppointment(String patientId, String doctorId, String specialty,
                                String date, String time, String reason, boolean byDoctor);

    Response acceptAppointment(String appointmentId, String doctorId);

    Response completeAppointment(String appointmentId, String doctorId,
                                 String diagnosis, String observations,
                                 String recommendedTreatment, String followUp);

    Response cancelAppointment(String appointmentId, String patientId);

    Response rescheduleAppointment(String appointmentId, String doctorId, String newTime, String rescheduleReason);

    Response prescribeMedication(String appointmentId, String doctorId, String medicationName, String dose,
                                 String administrationRoute, String duration, String frequency, String additionalInfo);
}
