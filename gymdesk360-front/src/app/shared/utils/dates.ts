import { DurationUnit } from '@/app/core/models/api.models';

/** Date local -> "YYYY-MM-DD" (sin pasar por UTC, para que no cambie el día). */
export function toIsoDate(d: Date | null | undefined): string | null {
    if (!d) return null;
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

/** "YYYY-MM-DD" -> Date local a medianoche. */
export function fromIsoDate(s: string | null | undefined): Date | null {
    if (!s) return null;
    const [y, m, d] = s.split('-').map(Number);
    return new Date(y, m - 1, d);
}

/** Hoy a medianoche local. */
export function today(): Date {
    const t = new Date();
    return new Date(t.getFullYear(), t.getMonth(), t.getDate());
}

export function addDays(d: Date, days: number): Date {
    return new Date(d.getFullYear(), d.getMonth(), d.getDate() + days);
}

/**
 * Último día (inclusive) de un plan que empieza en `start`. Igual que Plan.endDateFor del backend:
 * meses de calendario (31/01 + 1 mes = 28/02 - 1 día) o días corridos.
 */
export function planEndDate(start: Date, value: number, unit: DurationUnit): Date {
    if (unit === 'DAYS') return addDays(start, value - 1);
    const targetMonth = start.getMonth() + value;
    const lastDayOfTarget = new Date(start.getFullYear(), targetMonth + 1, 0).getDate();
    const sameDay = new Date(start.getFullYear(), targetMonth, Math.min(start.getDate(), lastDayOfTarget));
    return addDays(sameDay, -1);
}
