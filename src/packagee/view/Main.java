package packagee.view;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import packagee.controller.AppointmentController;
import packagee.controller.AuthController;
import packagee.controller.DoctorController;
import packagee.controller.HospitalizationController;
import packagee.controller.NavigationController;
import packagee.controller.PatientController;
import packagee.controller.interfaces.IAppointmentController;
import packagee.controller.interfaces.IAuthController;
import packagee.controller.interfaces.IDoctorController;
import packagee.controller.interfaces.IHospitalizationController;
import packagee.controller.interfaces.IPatientController;
import packagee.model.DataStore;

public final class Main {

    private Main() {}

    public static void main(String[] args) {
        System.setProperty("flatlaf.useNativeLibrary", "false");
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF: " + ex.getMessage());
        }
        SwingUtilities.invokeLater(() -> {
            DataStore ds = DataStore.getInstance();
            IAuthController auth = new AuthController(ds);
            IPatientController patient = new PatientController(ds);
            IDoctorController doctor = new DoctorController(ds);
            IAppointmentController appointment = new AppointmentController(ds);
            IHospitalizationController hospitalization = new HospitalizationController(ds);
            NavigationController nav = new NavigationController(auth, patient, doctor, appointment, hospitalization);
            nav.showLogin();
        });
    }
}
