/** Utilidades de RUT chileno (misma regla de módulo 11 que el backend). */

function clean(raw: string): string {
    return raw.replace(/[.\s-]/g, '').toUpperCase();
}

function computeDv(body: string): string {
    let sum = 0;
    let factor = 2;
    for (let i = body.length - 1; i >= 0; i--) {
        sum += Number(body[i]) * factor;
        factor = factor === 7 ? 2 : factor + 1;
    }
    const r = 11 - (sum % 11);
    return r === 11 ? '0' : r === 10 ? 'K' : String(r);
}

/** true si el RUT es válido (vacío también se considera válido: el campo es opcional). */
export function isValidRut(raw: string | null | undefined): boolean {
    if (!raw || !raw.trim()) return true;
    const c = clean(raw);
    if (!/^\d{7,8}[0-9K]$/.test(c)) return false;
    return computeDv(c.slice(0, -1)) === c.slice(-1);
}

/** "123456785" -> "12.345.678-5" (si no es válido lo devuelve tal cual). */
export function formatRut(raw: string | null | undefined): string {
    if (!raw) return '';
    const c = clean(raw);
    if (!/^\d{7,8}[0-9K]$/.test(c)) return raw;
    const body = c.slice(0, -1).replace(/\B(?=(\d{3})+(?!\d))/g, '.');
    return `${body}-${c.slice(-1)}`;
}
