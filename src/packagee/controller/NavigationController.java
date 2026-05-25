package packagee.controller;

import java.util.EnumMap;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import packagee.controller.interfaces.IAppointmentController;
import packagee.controller.interfaces.IAuthController;
import packagee.controller.interfaces.IDoctorController;
import packagee.controller.interfaces.IHospitalizationController;
import packagee.controller.interfaces.IPatientController;
import packagee.model.Role;
import packagee.model.observer.Observable;
import packagee.model.observer.Observer;
import packagee.view.AdminView;
import packagee.view.DoctorView;
import packagee.view.LoginView;
import packagee.view.PatientView;

public class NavigationController {

    @FunctionalInterface
    public interface ViewFactory {
        JFrame create(NavigationController nav, Map<String, Object> userInfo);
    }

    private final IAuthController auth;
    private final IPatientController patient;
    private final IDoctorController doctor;
    private final IAppointmentController appointment;
    private final IHospitalizationController hospitalization;
    private final Observable observable;
    private final EnumMap<Role, ViewFactory> viewFactories = new EnumMap<>(Role.class);

    public NavigationController(IAuthController auth, IPatientController patient,
                                IDoctorController doctor, IAppointmentController appointment,
                                IHospitalizationController hospitalization,
                                Observable observable) {
        this.auth = auth;
        this.patient = patient;
        this.doctor = doctor;
        this.appointment = appointment;
        this.hospitalization = hospitalization;
        this.observable = observable;

        registerViewFactory(Role.ADMIN,   (nav, info) -> new AdminView(nav, info, patient, doctor));
        registerViewFactory(Role.DOCTOR,  (nav, info) -> new DoctorView(nav, info, info, appointment, hospitalization, doctor));
        registerViewFactory(Role.PATIENT, (nav, info) -> new PatientView(nav, info, info, appointment, hospitalization, patient, doctor));
    }

    public final void registerViewFactory(Role role, ViewFactory factory) {
        viewFactories.put(role, factory);
    }

    public void subscribe(Observer observer) {
        observable.addObserver(observer);
    }

    public void unsubscribe(Observer observer) {
        observable.removeObserver(observer);
    }

    public void showLogin() {
        new LoginView(this, auth, patient).setVisible(true);
    }

    public void navigateAfterLogin(Map<String, Object> userInfo, JFrame from) {
        Role role = parseRole((String) userInfo.get("role"));
        ViewFactory factory = role == null ? null : viewFactories.get(role);
        if (factory == null) {
            JOptionPane.showMessageDialog(from,
                    "Unknown role: " + userInfo.get("role"), "Login error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JFrame next = factory.create(this, userInfo);
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

    private static Role parseRole(String name) {
        if (name == null) return null;
        try {
            return Role.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
