package com.smartemergency.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.smartemergency.domain.*;
import com.smartemergency.repo.*;

import java.time.LocalDate;
import java.util.List;

/** Inserts demo data (Cluj-Napoca) the first time the application starts on an empty database. */
@Component
public class DataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    public static final String DEMO_PASSWORD = "password123";

    private final UserRepository users;
    private final HospitalRepository hospitals;
    private final ResourceRepository resources;
    private final MedicalCardRepository cards;
    private final EmergencyContactRepository contacts;
    private final MedicalHistoryRepository history;
    private final PasswordEncoder encoder;
    private final boolean enabled;

    public DataSeeder(UserRepository users, HospitalRepository hospitals, ResourceRepository resources,
                      MedicalCardRepository cards, EmergencyContactRepository contacts,
                      MedicalHistoryRepository history, PasswordEncoder encoder,
                      @Value("${secc.seed-demo-data:true}") boolean enabled) {
        this.users = users;
        this.hospitals = hospitals;
        this.resources = resources;
        this.cards = cards;
        this.contacts = contacts;
        this.history = history;
        this.encoder = encoder;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled || users.count() > 0) return;
        log.info("Empty database - inserting demo data (password for all users: {})", DEMO_PASSWORD);

        Hospital h1 = hospitals.save(new Hospital("County Emergency Hospital Cluj", 46.7667, 23.5806));
        Hospital h2 = hospitals.save(new Hospital("Municipal Hospital Cluj", 46.7703, 23.5966));
        Hospital h3 = hospitals.save(new Hospital("Military Emergency Hospital Cluj", 46.7756, 23.6011));

        user("admin", "System Administrator", Role.ADMIN, null);
        user("dispatcher", "Andrei Pop", Role.DISPATCHER, null);
        user("dispatcher2", "Elena Muresan", Role.DISPATCHER, null);
        user("doctor1", "Dr. Ioana Rusu", Role.DOCTOR, h1);
        user("doctor2", "Dr. Mihai Stan", Role.DOCTOR, h2);
        user("doctor3", "Dr. Ana Varga", Role.DOCTOR, h3);

        // ambulances (with crew accounts) and other resources around the city
        Object[][] amb = {
                {"AMB-01", 46.7700, 23.5890}, {"AMB-02", 46.7840, 23.6100}, {"AMB-03", 46.7580, 23.5600},
                {"AMB-04", 46.7650, 23.6400}, {"AMB-05", 46.7900, 23.5700}, {"AMB-06", 46.7520, 23.6050},
                {"AMB-07", 46.7780, 23.6500}, {"AMB-08", 46.7450, 23.5850}};
        for (int i = 0; i < amb.length; i++) {
            Resource r = new Resource((String) amb[i][0], ResourceType.AMBULANCE, (double) amb[i][1], (double) amb[i][2]);
            if (i < 4) r.setCrewUser(user("crew" + (i + 1), "Crew " + amb[i][0], Role.CREW, null));
            resources.save(r);
        }
        resources.saveAll(List.of(
                new Resource("FT-01", ResourceType.FIRE_TEAM, 46.7690, 23.6000),
                new Resource("FT-02", ResourceType.FIRE_TEAM, 46.7600, 23.5700),
                new Resource("FT-03", ResourceType.FIRE_TEAM, 46.7850, 23.6300),
                new Resource("FTR-01", ResourceType.FIRE_TRUCK, 46.7692, 23.6003),
                new Resource("FTR-02", ResourceType.FIRE_TRUCK, 46.7602, 23.5703),
                new Resource("FTR-03", ResourceType.FIRE_TRUCK, 46.7852, 23.6303),
                new Resource("POL-01", ResourceType.POLICE_UNIT, 46.7710, 23.6150),
                new Resource("POL-02", ResourceType.POLICE_UNIT, 46.7550, 23.5950),
                new Resource("POL-03", ResourceType.POLICE_UNIT, 46.7880, 23.5900)));

        User p1 = user("patient1", "Ion Popescu", Role.PATIENT, null);
        card(p1, LocalDate.of(1958, 3, 14), "A+", "Penicillin", "Hypertension, Type 2 diabetes",
                "Metformin 850 mg x2/day, Enalapril 10 mg/day", "Pacemaker since 2019", 46.7790, 23.6050);
        contact(p1, "Maria Popescu", "Wife", "+40 744 123 456");
        contact(p1, "Radu Popescu", "Son", "+40 723 987 654");
        MedicalHistoryEntry e = new MedicalHistoryEntry();
        e.setPatientId(p1.getId());
        e.setEventDate(java.time.Instant.parse("2025-06-10T09:30:00Z"));
        e.setHospital(h1);
        e.setDiagnosis("Atrial fibrillation");
        e.setTreatment("Pacemaker check, anticoagulant treatment");
        history.save(e);

        User p2 = user("patient2", "Maria Ionescu", Role.PATIENT, null);
        card(p2, LocalDate.of(1992, 11, 2), "0-", "None known", "Asthma", "Salbutamol inhaler as needed",
                null, 46.7600, 23.5950);
        contact(p2, "George Ionescu", "Brother", "+40 755 111 222");

        User p3 = user("patient3", "Vlad Toma", Role.PATIENT, null);
        card(p3, LocalDate.of(2001, 7, 21), "B+", "Lactose", null, null, null, 46.7500, 23.6200);
    }

    private User user(String username, String name, Role role, Hospital h) {
        User u = new User();
        u.setUsername(username);
        u.setFullName(name);
        u.setRole(role);
        u.setHospital(h);
        u.setPasswordHash(encoder.encode(DEMO_PASSWORD));
        return users.save(u);
    }

    private void card(User p, LocalDate dob, String blood, String allergies, String conditions, String meds,
                      String notes, double lat, double lng) {
        MedicalCard c = new MedicalCard();
        c.setPatient(p);
        c.setDateOfBirth(dob);
        c.setBloodType(blood);
        c.setAllergies(allergies);
        c.setChronicConditions(conditions);
        c.setCurrentMedication(meds);
        c.setMedicalNotes(notes);
        c.setHomeLat(lat);
        c.setHomeLng(lng);
        cards.save(c);
    }

    private void contact(User p, String name, String rel, String phone) {
        EmergencyContact c = new EmergencyContact();
        c.setPatientId(p.getId());
        c.setName(name);
        c.setRelationship(rel);
        c.setPhone(phone);
        contacts.save(c);
    }
}
