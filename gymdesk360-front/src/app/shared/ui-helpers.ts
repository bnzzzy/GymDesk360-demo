import { DurationUnit, MembershipStatus, Role } from '@/app/core/models/api.models';

type Severity = 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast';

export const ROLE_SEVERITY: Record<Role, Severity> = {
    SUPER_ADMIN: 'contrast',
    ADMIN: 'warn',
    STAFF: 'info',
    MEMBER: 'secondary'
};

export const ROLE_OPTIONS: { label: string; value: Role }[] = [
    { label: 'Administrador', value: 'ADMIN' },
    { label: 'Staff', value: 'STAFF' },
    { label: 'Socio', value: 'MEMBER' },
    { label: 'Super admin', value: 'SUPER_ADMIN' }
];

/** "Mi Gimnasio Ñuñoa" -> "mi-gimnasio-nunoa" */
export function slugify(text: string): string {
    return text
        .normalize('NFD')
        .replace(/[̀-ͯ]/g, '')
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-+|-+$/g, '')
        .slice(0, 80);
}

export const STATUS_LABELS: Record<MembershipStatus, string> = {
    ACTIVE: 'Activo',
    UPCOMING: 'Por comenzar',
    EXPIRED: 'Vencido',
    CANCELLED: 'Anulada',
    NONE: 'Sin membresía'
};

export const STATUS_SEVERITY: Record<MembershipStatus, Severity> = {
    ACTIVE: 'success',
    UPCOMING: 'info',
    EXPIRED: 'danger',
    CANCELLED: 'secondary',
    NONE: 'secondary'
};

/** Filtros de estado para la lista de socios. */
export const MEMBER_STATUS_OPTIONS: { label: string; value: MembershipStatus }[] = [
    { label: 'Activos', value: 'ACTIVE' },
    { label: 'Por comenzar', value: 'UPCOMING' },
    { label: 'Vencidos', value: 'EXPIRED' },
    { label: 'Sin membresía', value: 'NONE' }
];

/** "1 mes", "3 meses", "1 día", "7 días" */
export function durationLabel(value: number, unit: DurationUnit): string {
    if (unit === 'MONTHS') return value === 1 ? '1 mes' : `${value} meses`;
    return value === 1 ? '1 día' : `${value} días`;
}

/** Iniciales para avatares: "José Muñoz" -> "JM" */
export function initials(name: string | null | undefined): string {
    return (name ?? '?')
        .split(/\s+/)
        .filter(Boolean)
        .slice(0, 2)
        .map((w) => w[0].toUpperCase())
        .join('');
}
