package com.benz.gymdesk360.util;

import com.benz.gymdesk360.exception.ApiException;
import com.benz.gymdesk360.security.AuthUser;

/** Todos los datos de negocio (planes, socios, membresías) se filtran por el gimnasio del usuario. */
public final class GymScope {

    private GymScope() {
    }

    public static Long gymId(AuthUser actor) {
        if (actor.gymId() == null) {
            throw ApiException.forbidden("Esta acción requiere pertenecer a un gimnasio");
        }
        return actor.gymId();
    }
}
