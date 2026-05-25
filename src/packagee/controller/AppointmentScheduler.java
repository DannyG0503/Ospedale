package packagee.controller;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import packagee.model.Appointment;
import packagee.model.AppointmentStatus;
import packagee.model.Doctor;
import packagee.model.IDataStore;
import packagee.model.Specialty;

public class AppointmentScheduler {

    public static final int APPOINTMENT_DURATION_MINUTES = 15;

    private final IDataStore store;

    public AppointmentScheduler(IDataStore store) {
        this.store = store;
    }

    public boolean isDoctorAvailable(Doctor d, LocalDateTime when, Appointment ignore) {
        for (Appointment a : store.getAppointments()) {
            if (ignore != null && a == ignore) continue;
            if (a.getDoctor() == null || a.getDoctor().getId() != d.getId()) continue;
            AppointmentStatus s = a.getStatus();
            if (s != AppointmentStatus.REQUESTED && s != AppointmentStatus.PENDING) continue;
            long diff = Math.abs(ChronoUnit.MINUTES.between(a.getDatetime(), when));
            if (diff < APPOINTMENT_DURATION_MINUTES) return false;
        }
        return true;
    }

    public Doctor findAvailableDoctorBySpecialty(Specialty s, LocalDateTime when) {
        for (Doctor d : store.getDoctors()) {
            if (d.getSpecialty() == s && isDoctorAvailable(d, when, null)) return d;
        }
        return null;
    }

    public String nextAppointmentId(long patientId) {
        String prefix = "A-" + patientId + "-";
        int max = -1;
        for (Appointment a : store.getAppointments()) {
            String aid = a.getId();
            if (aid != null && aid.startsWith(prefix)) {
                try {
                    max = Math.max(max, Integer.parseInt(aid.substring(prefix.length())));
                } catch (NumberFormatException ignored) {}
            }
        }
        return String.format("%s%04d", prefix, max + 1);
    }
}
