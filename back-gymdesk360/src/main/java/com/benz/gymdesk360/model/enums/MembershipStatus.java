package com.benz.gymdesk360.model.enums;

/** Estado de una membresía o, aplicado a un socio, el de su membresía más relevante. */
public enum MembershipStatus {
    /** Vigente hoy. */
    ACTIVE,
    /** Pagada pero aún no comienza. */
    UPCOMING,
    /** Ya terminó. */
    EXPIRED,
    /** Anulada. */
    CANCELLED,
    /** (Solo socios) nunca ha tenido una membresía. */
    NONE
}
