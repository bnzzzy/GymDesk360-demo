package com.benz.gymdesk360.demo;

import com.benz.gymdesk360.model.Gym;
import com.benz.gymdesk360.model.Member;
import com.benz.gymdesk360.model.Membership;
import com.benz.gymdesk360.model.Plan;
import com.benz.gymdesk360.model.User;
import com.benz.gymdesk360.model.enums.DurationUnit;
import com.benz.gymdesk360.model.enums.Role;
import com.benz.gymdesk360.repository.GymRepository;
import com.benz.gymdesk360.repository.MemberRepository;
import com.benz.gymdesk360.repository.MembershipRepository;
import com.benz.gymdesk360.repository.PlanRepository;
import com.benz.gymdesk360.repository.UserRepository;
import com.benz.gymdesk360.util.RutUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Datos de la demo pública. Con app.demo.enabled=true:
 * - al arrancar, si no existe el gimnasio demo, carga los datos;
 * - todos los días a las 04:30 (hora de Chile) BORRA toda la base y la vuelve a cargar.
 * Las fechas se generan relativas a "hoy", así la demo siempre tiene socios activos, por vencer y vencidos.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataService implements ApplicationRunner {

    private final DemoGuard demoGuard;
    private final JdbcTemplate jdbc;
    private final GymRepository gymRepository;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final MemberRepository memberRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final PlatformTransactionManager txManager;

    private Random rnd;
    private LocalDate today;
    private ZoneId zone;

    @Override
    public void run(ApplicationArguments args) {
        if (demoGuard.enabled() && !gymRepository.existsBySlug(DemoGuard.MAIN_GYM_SLUG)) {
            reset();
        }
    }

    @Scheduled(cron = "${app.demo.reset-cron:0 30 4 * * *}", zone = "America/Santiago")
    public void scheduledReset() {
        if (demoGuard.enabled()) {
            reset();
        }
    }

    /** Todo en una transacción: si algo falla, la demo queda como estaba. */
    public synchronized void reset() {
        if (!demoGuard.enabled()) {
            throw new IllegalStateException("El reinicio de datos solo existe en modo demo");
        }
        new TransactionTemplate(txManager).executeWithoutResult(status -> load());
    }

    private void load() {
        long t0 = System.currentTimeMillis();
        jdbc.execute("TRUNCATE memberships, members, plans, refresh_tokens, users, gyms RESTART IDENTITY CASCADE");

        today = LocalDate.now(clock);
        zone = clock.getZone();
        rnd = new Random(today.toEpochDay()); // cambia cada día, pero es reproducible

        user(null, DemoGuard.SUPER_ADMIN_EMAIL, "Sofía Valenzuela", Role.SUPER_ADMIN, 300);

        // Gimnasio principal: el que se recorre con las cuentas admin / recepción
        Gym main = gym("Iron Box Demo", DemoGuard.MAIN_GYM_SLUG, true, 240);
        User admin = user(main, DemoGuard.ADMIN_EMAIL, "Camila Rojas", Role.ADMIN, 240);
        User staff = user(main, DemoGuard.STAFF_EMAIL, "Diego Fuentes", Role.STAFF, 200);
        user(main, "matias.coach@ironbox.cl", "Matías Herrera", Role.STAFF, 90);
        List<Plan> plans = List.of(
                plan(main, "Mensual libre", "Acceso ilimitado a sala de máquinas y clases grupales.", 25_000, 1, DurationUnit.MONTHS, true),
                plan(main, "Trimestral", "Tres meses con acceso ilimitado. Ahorra $10.000.", 65_000, 3, DurationUnit.MONTHS, true),
                plan(main, "Semestral", "Seis meses con acceso ilimitado y evaluación física incluida.", 120_000, 6, DurationUnit.MONTHS, true),
                plan(main, "Anual", "Doce meses con acceso ilimitado, evaluación física y polera.", 220_000, 12, DurationUnit.MONTHS, true),
                plan(main, "Estudiante mensual", "Acceso de lunes a viernes hasta las 17:00. Requiere credencial.", 18_000, 1, DurationUnit.MONTHS, true),
                plan(main, "Pase semanal", "Siete días corridos de acceso libre.", 8_000, 7, DurationUnit.DAYS, true),
                plan(main, "Pase diario", "Un día de entrenamiento.", 3_000, 1, DurationUnit.DAYS, true),
                plan(main, "Promo verano", "Promoción de temporada (ya no se vende).", 20_000, 1, DurationUnit.MONTHS, false));
        seedMembers(main, plans, 46, List.of(admin.getId(), staff.getId()));

        // Otros clientes de la plataforma, para que el panel del super admin tenga datos
        Gym andes = gym("CrossFit Andes", "crossfit-andes", true, 150);
        User andesAdmin = user(andes, "admin@crossfitandes.cl", "Tomás Castro", Role.ADMIN, 150);
        List<Plan> andesPlans = List.of(
                plan(andes, "Mensual WOD", "Clases de CrossFit ilimitadas.", 45_000, 1, DurationUnit.MONTHS, true),
                plan(andes, "Trimestral WOD", "Tres meses de clases ilimitadas.", 120_000, 3, DurationUnit.MONTHS, true),
                plan(andes, "Drop-in", "Una clase suelta.", 8_000, 1, DurationUnit.DAYS, true));
        seedMembers(andes, andesPlans, 14, List.of(andesAdmin.getId()));

        Gym pilates = gym("Studio Pilates Vitacura", "studio-pilates-vitacura", true, 70);
        User pilatesAdmin = user(pilates, "hola@pilatesvitacura.cl", "Valentina Soto", Role.ADMIN, 70);
        List<Plan> pilatesPlans = List.of(
                plan(pilates, "8 clases al mes", "Reformer, dos veces por semana.", 70_000, 1, DurationUnit.MONTHS, true),
                plan(pilates, "12 clases al mes", "Reformer, tres veces por semana.", 95_000, 1, DurationUnit.MONTHS, true));
        seedMembers(pilates, pilatesPlans, 9, List.of(pilatesAdmin.getId()));

        Gym closed = gym("Box Ñuñoa (cerrado)", "box-nunoa", false, 330);
        user(closed, "contacto@boxnunoa.cl", "Ignacio Pérez", Role.ADMIN, 330);

        log.info("Demo reiniciada: {} gimnasios, {} socios, {} membresías en {} ms", gymRepository.count(),
                memberRepository.count(), membershipRepository.count(), System.currentTimeMillis() - t0);
    }

    // ---------------------------------------------------------------- socios y membresías

    private enum Profile { ACTIVE, EXPIRING, RENEWED, UPCOMING, EXPIRED_RECENT, EXPIRED_OLD, NONE }

    private void seedMembers(Gym gym, List<Plan> plans, int count, List<Long> sellers) {
        List<Plan> sellable = plans.stream().filter(Plan::isActive).toList();
        List<Plan> monthly = sellable.stream().filter(p -> p.getDurationUnit() == DurationUnit.MONTHS).toList();
        List<Profile> profiles = profiles(count);
        Membership cancelCandidate = null;

        for (int i = 0; i < count; i++) {
            Profile profile = profiles.get(i);
            Member m = newMember(gym);
            // Plan principal: casi siempre mensual (el más común), a veces más largo o un pase
            Plan plan = rnd.nextInt(10) < 6 ? monthly.get(0) : monthly.get(rnd.nextInt(monthly.size()));
            if (profile == Profile.EXPIRED_OLD && rnd.nextBoolean()) {
                plan = sellable.get(sellable.size() - 1 - rnd.nextInt(Math.min(2, sellable.size())));
            }

            List<Membership> chain = new ArrayList<>();
            LocalDate firstStart = null;
            switch (profile) {
                case ACTIVE -> {
                    // Termina entre 8 días y ~1 mes: lejos de vencer, pero ya empezado aunque el mes anterior sea corto
                    LocalDate end = today.plusDays(8 + rnd.nextInt(Math.max(1, approxDays(plan) - 11)));
                    firstStart = startForEnd(plan, end);
                }
                case EXPIRING, RENEWED -> firstStart = startForEnd(plan, today.plusDays(rnd.nextInt(7)));
                case UPCOMING -> firstStart = today.plusDays(1 + rnd.nextInt(6));
                case EXPIRED_RECENT -> firstStart = startForEnd(plan, today.minusDays(1 + rnd.nextInt(27)));
                case EXPIRED_OLD -> firstStart = startForEnd(plan, today.minusDays(35 + rnd.nextInt(120)));
                case NONE -> {
                }
            }

            if (firstStart != null) {
                chain.add(membership(gym, m, plan, firstStart, sellers));
                // Historial: renovaciones anteriores pegadas, hasta ~8 meses atrás
                int previous = profile == Profile.UPCOMING ? 0 : rnd.nextInt(5);
                LocalDate start = firstStart;
                for (int k = 0; k < previous; k++) {
                    LocalDate prevStart = previousStart(plan, start);
                    if (prevStart.isBefore(today.minusDays(240))) break;
                    chain.add(membership(gym, m, plan, prevStart, sellers));
                    start = prevStart;
                }
                if (profile == Profile.RENEWED) {
                    Membership current = chain.get(0);
                    Membership renewal = membership(gym, m, plan, current.getEndDate().plusDays(1), sellers);
                    renewal.setCreatedAt(at(today.minusDays(rnd.nextInt(3))));
                    chain.add(renewal);
                }
            }

            LocalDate earliest = chain.stream().map(Membership::getStartDate).min(LocalDate::compareTo)
                    .orElse(today.minusDays(rnd.nextInt(20)));
            m.setCreatedAt(at(earliest.minusDays(rnd.nextInt(3))));
            if (profile == Profile.EXPIRED_OLD && rnd.nextInt(3) == 0) {
                m.setActive(false); // ficha desactivada (dejó de venir)
            }
            memberRepository.save(m);
            membershipRepository.saveAll(chain);
            if (cancelCandidate == null && chain.size() > 2) {
                cancelCandidate = chain.get(chain.size() - 1);
            }
        }

        if (cancelCandidate != null) {
            cancelCandidate.setCancelledAt(cancelCandidate.getCreatedAt().plus(1, ChronoUnit.DAYS));
            cancelCandidate.setNotes("Anulada: se registró dos veces por error");
        }
    }

    /** Reparto de estados para que la demo muestre de todo. */
    private List<Profile> profiles(int count) {
        List<Profile> list = new ArrayList<>();
        int[][] weights = {
                {Profile.ACTIVE.ordinal(), 42}, {Profile.EXPIRING.ordinal(), 12}, {Profile.RENEWED.ordinal(), 8},
                {Profile.UPCOMING.ordinal(), 5}, {Profile.EXPIRED_RECENT.ordinal(), 13}, {Profile.EXPIRED_OLD.ordinal(), 10},
                {Profile.NONE.ordinal(), 10}};
        for (int[] w : weights) {
            long n = Math.round(count * w[1] / 100.0);
            for (int i = 0; i < n; i++) list.add(Profile.values()[w[0]]);
        }
        while (list.size() < count) list.add(Profile.ACTIVE);
        while (list.size() > count) list.remove(list.size() - 1);
        Collections.shuffle(list, rnd);
        return list;
    }

    private Membership membership(Gym gym, Member member, Plan plan, LocalDate start, List<Long> sellers) {
        Membership ms = new Membership();
        ms.setGymId(gym.getId());
        ms.setMember(member);
        ms.setPlanId(plan.getId());
        ms.setPlanName(plan.getName());
        ms.setStartDate(start);
        ms.setEndDate(plan.endDateFor(start));
        boolean discount = plan.getPrice() >= 20_000 && rnd.nextInt(7) == 0;
        ms.setPricePaid(discount ? Math.round(plan.getPrice() * 0.9 / 1000.0) * 1000 : plan.getPrice());
        ms.setNotes(discount ? "Descuento 10% por referido" : pick(SALE_NOTES));
        ms.setCreatedBy(sellers.get(rnd.nextInt(sellers.size())));
        LocalDate sold = start.isAfter(today) ? today.minusDays(rnd.nextInt(3)) : start.minusDays(rnd.nextInt(3));
        ms.setCreatedAt(at(sold));
        return ms;
    }

    private Member newMember(Gym gym) {
        boolean female = rnd.nextBoolean();
        String first = pick(female ? FEMALE_NAMES : MALE_NAMES);
        String last = pick(LAST_NAMES) + " " + pick(LAST_NAMES);
        Member m = new Member();
        m.setGymId(gym.getId());
        m.setFirstName(first);
        m.setLastName(last);
        if (rnd.nextInt(10) < 8) {
            String body = String.valueOf(12_000_000 + rnd.nextInt(13_000_000));
            m.setRut(body + "-" + RutUtils.computeDv(body));
        }
        m.setPhone(String.format("+569%08d", rnd.nextInt(100_000_000)));
        if (rnd.nextInt(10) < 7) {
            m.setEmail(ascii(first + "." + last.split(" ")[0]) + rnd.nextInt(100) + "@" + pick(EMAIL_DOMAINS));
        }
        if (rnd.nextInt(10) < 7) {
            m.setBirthDate(today.minusYears(18 + rnd.nextInt(45)).minusDays(rnd.nextInt(365)));
        }
        if (rnd.nextInt(10) < 5) {
            m.setEmergencyContactName(pick(rnd.nextBoolean() ? FEMALE_NAMES : MALE_NAMES) + " " + last.split(" ")[0]);
            m.setEmergencyContactPhone(String.format("+569%08d", rnd.nextInt(100_000_000)));
        }
        if (rnd.nextInt(10) < 3) {
            m.setNotes(pick(MEMBER_NOTES));
        }
        return m;
    }

    // ---------------------------------------------------------------- fechas

    /** Inicio que hace terminar el plan exactamente en {@code end} (inverso de Plan.endDateFor). */
    private static LocalDate startForEnd(Plan plan, LocalDate end) {
        return plan.getDurationUnit() == DurationUnit.MONTHS
                ? end.plusDays(1).minusMonths(plan.getDurationValue())
                : end.minusDays(plan.getDurationValue() - 1L);
    }

    private static LocalDate previousStart(Plan plan, LocalDate start) {
        return plan.getDurationUnit() == DurationUnit.MONTHS
                ? start.minusMonths(plan.getDurationValue())
                : start.minusDays(plan.getDurationValue());
    }

    private static int approxDays(Plan plan) {
        return plan.getDurationUnit() == DurationUnit.MONTHS ? plan.getDurationValue() * 30 : plan.getDurationValue();
    }

    /** Instante "de horario de atención" (9:00 a 20:59) de ese día; nunca en el futuro. */
    private Instant at(LocalDate day) {
        Instant t = day.atTime(LocalTime.of(9 + rnd.nextInt(12), rnd.nextInt(60))).atZone(zone).toInstant();
        Instant now = Instant.now(clock);
        return t.isAfter(now) ? now.minus(rnd.nextInt(120) + 1, ChronoUnit.MINUTES) : t;
    }

    // ---------------------------------------------------------------- entidades base

    private Gym gym(String name, String slug, boolean active, int daysAgo) {
        Gym g = new Gym();
        g.setName(name);
        g.setSlug(slug);
        g.setActive(active);
        g.setCreatedAt(at(today.minusDays(daysAgo)));
        return gymRepository.save(g);
    }

    /** Las cuentas demo tienen una contraseña aleatoria que nadie conoce: se entra con /api/auth/demo/{cuenta}. */
    private User user(Gym gym, String email, String fullName, Role role, int daysAgo) {
        byte[] secret = new byte[24];
        new SecureRandom().nextBytes(secret);
        User u = new User();
        u.setGym(gym);
        u.setEmail(email);
        u.setFullName(fullName);
        u.setRole(role);
        u.setPasswordHash(passwordEncoder.encode(Base64.getEncoder().encodeToString(secret)));
        u.setCreatedAt(at(today.minusDays(daysAgo)));
        return userRepository.save(u);
    }

    private Plan plan(Gym gym, String name, String description, long price, int value, DurationUnit unit, boolean active) {
        Plan p = new Plan();
        p.setGymId(gym.getId());
        p.setName(name);
        p.setDescription(description);
        p.setPrice(price);
        p.setDurationValue(value);
        p.setDurationUnit(unit);
        p.setActive(active);
        p.setCreatedAt(at(today.minusDays(200)));
        return planRepository.save(p);
    }

    private <T> T pick(List<T> list) {
        return list.get(rnd.nextInt(list.size()));
    }

    private static String ascii(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z.]", "");
    }

    // ---------------------------------------------------------------- datos ficticios

    private static final List<String> FEMALE_NAMES = List.of("Camila", "Valentina", "Javiera", "Catalina", "Fernanda",
            "Constanza", "Antonia", "Isidora", "Francisca", "Martina", "Sofía", "Josefa", "Daniela", "Trinidad", "Florencia",
            "Amanda", "Paula", "Macarena", "Bárbara", "Ignacia");
    private static final List<String> MALE_NAMES = List.of("Matías", "Benjamín", "Vicente", "Martín", "Joaquín",
            "Sebastián", "Tomás", "Agustín", "Cristóbal", "Diego", "Nicolás", "Felipe", "Ignacio", "Maximiliano", "Lucas",
            "Gabriel", "Rodrigo", "Andrés", "Francisco", "José");
    private static final List<String> LAST_NAMES = List.of("González", "Muñoz", "Rojas", "Díaz", "Pérez", "Soto",
            "Contreras", "Silva", "Martínez", "Sepúlveda", "Morales", "Rodríguez", "López", "Fuentes", "Hernández",
            "Torres", "Araya", "Flores", "Espinoza", "Valenzuela", "Castillo", "Tapia", "Reyes", "Gutiérrez", "Castro",
            "Pizarro", "Álvarez", "Vásquez", "Sánchez", "Fernández");
    private static final List<String> EMAIL_DOMAINS = List.of("gmail.com", "gmail.com", "gmail.com", "hotmail.com", "outlook.com", "yahoo.com");
    private static final List<String> MEMBER_NOTES = List.of(
            "Lesión de rodilla derecha: evitar sentadilla profunda.",
            "Objetivo: bajar 8 kg antes del verano.",
            "Viene en horario AM, antes del trabajo.",
            "Preparándose para su primera maratón.",
            "Asmática: lleva inhalador.",
            "Quiere clases de técnica de levantamiento.",
            "Referido por otro socio.");
    private static final List<String> SALE_NOTES = java.util.Arrays.asList(
            null, null, null, null, "Pagó con transferencia", "Pagó en efectivo", "Pagó con débito", "Pagó con crédito");
}
