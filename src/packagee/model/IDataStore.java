package packagee.model;

import java.util.List;
import packagee.model.observer.Observable;

public interface IDataStore extends Observable {

    List<IUser> getUsers();

    List<Patient> getPatients();

    List<Doctor> getDoctors();

    List<Appointment> getAppointments();

    List<Hospitalization> getHospitalizations();

    boolean addPatient(Patient patient);

    boolean addDoctor(Doctor doctor);

    boolean addAdministrator(Administrator admin);

    boolean addAppointment(Appointment appt);

    boolean addHospitalization(Hospitalization hosp);

    IUser findUserByUsername(String username);

    Patient findPatientById(long id);

    Doctor findDoctorById(long id);

    Appointment findAppointmentById(String id);

    Hospitalization findHospitalizationById(String id);
}
