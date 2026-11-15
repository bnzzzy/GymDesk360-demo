package com.benz.gymdesk360.demo;

import com.benz.gymdesk360.exception.ApiException;
import com.benz.gymdesk360.model.Gym;
import com.benz.gymdesk360.model.User;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Reglas del modo demo. Los visitantes pueden usar todo, salvo dejar inutilizable la demo para el siguiente:
 * no se pueden modificar las cuentas de demostración ni el gimnasio principal.
 * Fuera del modo demo todos los métodos son no-op.
 */
@Component
public class DemoGuard {

    public static final String DOMAIN = "@demo.gymdesk360.cl";
    public static final String SUPER_ADMIN_EMAIL = "superadmin" + DOMAIN;
    public static final String ADMIN_EMAIL = "admin" + DOMAIN;
    public static final String STAFF_EMAIL = "recepcion" + DOMAIN;
    public static final String MAIN_GYM_SLUG = "iron-box-demo";

    private static final Map<String, String> ACCOUNTS = Map.of(
            "super-admin", SUPER_ADMIN_EMAIL,
            "admin", ADMIN_EMAIL,
            "staff", STAFF_EMAIL);

    private final boolean enabled;

    public DemoGuard(@Value("${app.demo.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    public boolean enabled() {
        return enabled;
    }

    /** Email de la cuenta demo pedida ("super-admin", "admin", "staff"). 404 si no hay demo o no existe. */
    public String demoAccountEmail(String account) {
        String email = enabled ? ACCOUNTS.get(account) : null;
        if (email == null) {
            throw ApiException.notFound("Recurso no encontrado");
        }
        return email;
    }

    public void protectUser(User user) {
        if (enabled && user.getEmail().endsWith(DOMAIN)) {
            throw ApiException.forbidden("En la demo no se pueden modificar las cuentas de demostración");
        }
    }

    public void protectGym(Gym gym) {
        if (enabled && MAIN_GYM_SLUG.equals(gym.getSlug())) {
            throw ApiException.forbidden("En la demo no se puede modificar el gimnasio principal");
        }
    }
}
