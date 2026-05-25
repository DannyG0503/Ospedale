package packagee.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import packagee.controller.serializer.AppointmentSerializer;
import packagee.controller.serializer.DoctorSerializer;
import packagee.controller.serializer.HospitalizationSerializer;
import packagee.controller.serializer.PatientSerializer;
import packagee.model.observer.Observer;

public final class DataStore implements IDataStore {

    private static final String USERS_JSON = "json/users.json";
    private static final String APPOINTMENTS_JSON = "json/appointments.json";
    private static final String HOSPITALIZATIONS_JSON = "json/hospitalizations.json";

    private static DataStore instance;

    private final ArrayList<IUser> users;
    private final ArrayList<Patient> patients;
    private final ArrayList<Doctor> doctors;
    private final ArrayList<Appointment> appointments;
    private final ArrayList<Hospitalization> hospitalizations;
    private final List<Observer> observers;

    private DataStore() {
        this.users = new ArrayList<>();
        this.patients = new ArrayList<>();
        this.doctors = new ArrayList<>();
        this.appointments = new ArrayList<>();
        this.hospitalizations = new ArrayList<>();
        this.observers = new ArrayList<>();

        seedDefaultAdmin();
        loadUsers(USERS_JSON);
        loadAppointments(APPOINTMENTS_JSON);
        loadHospitalizations(HOSPITALIZATIONS_JSON);
    }

    public static synchronized DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    // ----- Getters (defensive copies) -----

    @Override
    public List<IUser> getUsers() {
        return new ArrayList<>(users);
    }

    @Override
    public List<Patient> getPatients() {
        return new ArrayList<>(patients);
    }

    @Override
    public List<Doctor> getDoctors() {
        return new ArrayList<>(doctors);
    }

    @Override
    public List<Appointment> getAppointments() {
        return new ArrayList<>(appointments);
    }

    @Override
    public List<Hospitalization> getHospitalizations() {
        return new ArrayList<>(hospitalizations);
    }

    // ----- Mutators -----

    @Override
    public boolean addPatient(Patient patient) {
        if (patient == null || findUserByUsername(patient.getUsername()) != null) {
            return false;
        }
        patients.add(patient);
        users.add(patient);
        notifyObservers("PATIENT_ADDED", PatientSerializer.serialize(patient));
        return true;
    }

    @Override
    public boolean addDoctor(Doctor doctor) {
        if (doctor == null || findUserByUsername(doctor.getUsername()) != null) {
            return false;
        }
        doctors.add(doctor);
        users.add(doctor);
        notifyObservers("DOCTOR_ADDED", DoctorSerializer.serialize(doctor));
        return true;
    }

    @Override
    public boolean addAdministrator(Administrator admin) {
        if (admin == null || findUserByUsername(admin.getUsername()) != null) {
            return false;
        }
        users.add(admin);
        return true;
    }

    @Override
    public boolean addAppointment(Appointment appt) {
        if (appt == null || findAppointmentById(appt.getId()) != null) {
            return false;
        }
        appointments.add(appt);
        if (appt.getPatient() != null) {
            appt.getPatient().addAppointment(appt);
        }
        if (appt.getDoctor() != null) {
            appt.getDoctor().addAppointment(appt);
        }
        notifyObservers("APPOINTMENT_ADDED", AppointmentSerializer.serialize(appt));
        return true;
    }

    @Override
    public boolean addHospitalization(Hospitalization hosp) {
        if (hosp == null || findHospitalizationById(hosp.getId()) != null) {
            return false;
        }
        hospitalizations.add(hosp);
        if (hosp.getPatient() != null) {
            hosp.getPatient().setHospitalization(hosp);
        }
        if (hosp.getDoctor() != null) {
            hosp.getDoctor().addHospitalization(hosp);
        }
        notifyObservers("HOSPITALIZATION_ADDED", HospitalizationSerializer.serialize(hosp));
        return true;
    }

    // ----- Observable -----

    @Override
    public void addObserver(Observer observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(String event, Object data) {
        for (Observer o : new ArrayList<>(observers)) {
            try {
                o.update(event, data);
            } catch (RuntimeException ex) {
                System.err.println("DataStore: observer threw on event " + event + ": " + ex.getMessage());
            }
        }
    }

    // ----- Finders -----

    @Override
    public IUser findUserByUsername(String username) {
        if (username == null) return null;
        for (IUser u : users) {
            if (username.equals(u.getUsername())) {
                return u;
            }
        }
        return null;
    }

    @Override
    public Patient findPatientById(long id) {
        for (Patient p : patients) {
            if (p.getId() == id) return p;
        }
        return null;
    }

    @Override
    public Doctor findDoctorById(long id) {
        for (Doctor d : doctors) {
            if (d.getId() == id) return d;
        }
        return null;
    }

    @Override
    public Appointment findAppointmentById(String id) {
        if (id == null) return null;
        for (Appointment a : appointments) {
            if (id.equals(a.getId())) return a;
        }
        return null;
    }

    @Override
    public Hospitalization findHospitalizationById(String id) {
        if (id == null) return null;
        for (Hospitalization h : hospitalizations) {
            if (id.equals(h.getId())) return h;
        }
        return null;
    }

    // ----- Seed / loaders -----

    private void seedDefaultAdmin() {
        users.add(new Administrator(0L, "admin", "admin", "admin", "admin123"));
    }

    private void loadUsers(String path) {
        JSONObject root = readJsonObject(path);
        if (root == null || !root.has("users")) return;
        JSONArray arr = root.getJSONArray("users");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            String type = o.optString("type", "");
            try {
                switch (type) {
                    case "admin"   -> addAdministrator(parseAdmin(o));
                    case "patient" -> addPatient(parsePatient(o));
                    case "doctor"  -> addDoctor(parseDoctor(o));
                    default -> System.err.println("DataStore: unknown user type '" + type + "' at index " + i);
                }
            } catch (RuntimeException ex) {
                System.err.println("DataStore: failed to parse user at index " + i + ": " + ex.getMessage());
            }
        }
    }

    private Administrator parseAdmin(JSONObject o) {
        return new Administrator(
                o.getLong("id"),
                o.getString("username"),
                o.getString("firstname"),
                o.getString("lastname"),
                o.getString("password"));
    }

    private Patient parsePatient(JSONObject o) {
        return new Patient(
                o.getLong("id"),
                o.getString("username"),
                o.getString("firstname"),
                o.getString("lastname"),
                o.getString("password"),
                o.getString("email"),
                LocalDate.parse(o.getString("birthdate")),
                o.getBoolean("gender"),
                o.getLong("phone"),
                o.getString("address"));
    }

    private Doctor parseDoctor(JSONObject o) {
        return new Doctor(
                o.getLong("id"),
                o.getString("username"),
                o.getString("firstname"),
                o.getString("lastname"),
                o.getString("password"),
                Specialty.valueOf(o.getString("specialty")),
                o.getString("licenceNumber"),
                o.getString("assignedOffice"));
    }

    private void loadAppointments(String path) {
        JSONObject root = readJsonObject(path);
        if (root == null || !root.has("appointments")) return;
        JSONArray arr = root.getJSONArray("appointments");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            try {
                Patient patient = findPatientById(o.getLong("patientId"));
                Doctor doctor = findDoctorById(o.getLong("doctorId"));
                if (patient == null || doctor == null) {
                    System.err.println("DataStore: appointment " + o.optString("id") + " references unknown patient/doctor");
                    continue;
                }
                Appointment a = new Appointment(
                        o.getString("id"),
                        patient,
                        doctor,
                        Specialty.valueOf(o.getString("specialty")),
                        LocalDateTime.parse(o.getString("datetime")),
                        o.optString("reason", ""),
                        o.optBoolean("type", false));
                if (o.has("status")) {
                    a.setStatus(AppointmentStatus.valueOf(o.getString("status")));
                }
                addAppointment(a);
            } catch (RuntimeException ex) {
                System.err.println("DataStore: failed to parse appointment at index " + i + ": " + ex.getMessage());
            }
        }
    }

    private void loadHospitalizations(String path) {
        JSONObject root = readJsonObject(path);
        if (root == null || !root.has("hospitalizations")) return;
        JSONArray arr = root.getJSONArray("hospitalizations");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            try {
                Patient patient = findPatientById(o.getLong("patientId"));
                Doctor doctor = findDoctorById(o.getLong("doctorId"));
                if (patient == null || doctor == null) {
                    System.err.println("DataStore: hospitalization " + o.optString("id") + " references unknown patient/doctor");
                    continue;
                }
                HospitalizationStatus status = o.has("status")
                        ? HospitalizationStatus.valueOf(o.getString("status"))
                        : HospitalizationStatus.REQUESTED;
                Hospitalization h = new Hospitalization(
                        o.getString("id"),
                        patient,
                        doctor,
                        LocalDate.parse(o.getString("date")),
                        o.optString("reason", ""),
                        RoomType.valueOf(o.getString("roomType")),
                        o.optString("observations", ""),
                        status);
                addHospitalization(h);
            } catch (RuntimeException ex) {
                System.err.println("DataStore: failed to parse hospitalization at index " + i + ": " + ex.getMessage());
            }
        }
    }

    private JSONObject readJsonObject(String path) {
        Path p = Paths.get(path);
        if (!Files.exists(p)) {
            return null;
        }
        try {
            String content = Files.readString(p);
            return new JSONObject(content);
        } catch (IOException ex) {
            System.err.println("DataStore: failed to read " + path + ": " + ex.getMessage());
            return null;
        }
    }
}
