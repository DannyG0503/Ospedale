package packagee.controller;

import java.util.Map;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import packagee.controller.interfaces.IAppointmentController;
import packagee.controller.interfaces.IAuthController;
import packagee.controller.interfaces.IDoctorController;
import packagee.controller.interfaces.IHospitalizationController;
import packagee.controller.interfaces.IPatientController;
import packagee.view.AdminView;
import packagee.view.DoctorView;
import packagee.view.LoginView;
import packagee.view.PatientView;

public class NavigationController {

    private final IAuthController auth;
    private final IPatientController patient;
    private final IDoctorController doctor;
    private final IAppointmentController appointment;
    private final IHospitalizationController hospitalization;

    public NavigationController(IAuthController auth, IPatientController patient,
                                IDoctorController doctor, IAppointmentController appointment,
                                IHospitalizationController hospitalization) {
        this.auth = auth;
        this.patient = patient;
        this.doctor = doctor;
        this.appointment = appointment;
        this.hospitalization = hospitalization;
    }

    public void showLogin() {
        new LoginView(this, auth, patient).setVisible(true);
    }

    public void navigateAfterLogin(Map<String, Object> userInfo, JFrame from) {
        JFrame next;
        switch ((String) userInfo.get("role")) {
            case "ADMIN":
                next = new AdminView(this, userInfo, patient, doctor);
                break;
            case "DOCTOR":
                next = new DoctorView(this, userInfo, userInfo, appointment, hospitalization, doctor);
                break;
            case "PATIENT":
                next = new PatientView(this, userInfo, userInfo, appointment, hospitalization, patient, doctor);
                break;
            default:
                JOptionPane.showMessageDialog(from,
                        "Unknown role: " + userInfo.get("role"), "Login error", JOptionPane.ERROR_MESSAGE);
                return;
        }
        next.setVisible(true);
        from.dispose();
    }

    public void showPatientView(Map<String, Object> loggedIn, Map<String, Object> target, JFrame from) {
        new PatientView(this, loggedIn, target, appointment, hospitalization, patient, doctor).setVisible(true);
        from.dispose();
    }

    public void showDoctorView(Map<String, Object> loggedIn, Map<String, Object> target, JFrame from) {
        new DoctorView(this, loggedIn, target, appointment, hospitalization, doctor).setVisible(true);
        from.dispose();
    }

    public void showAdminView(Map<String, Object> loggedIn, JFrame from) {
        new AdminView(this, loggedIn, patient, doctor).setVisible(true);
        from.dispose();
    }

    public void logout(JFrame from) {
        showLogin();
        from.dispose();
    }
}
