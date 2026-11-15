export type Role = 'SUPER_ADMIN' | 'ADMIN' | 'STAFF' | 'MEMBER';

export const ROLE_LABELS: Record<Role, string> = {
    SUPER_ADMIN: 'Super admin',
    ADMIN: 'Administrador',
    STAFF: 'Staff',
    MEMBER: 'Socio'
};

export interface User {
    id: number;
    gymId: number | null;
    gymName: string | null;
    email: string;
    fullName: string;
    role: Role;
    enabled: boolean;
    createdAt: string;
}

export interface AuthResponse {
    accessToken: string;
    refreshToken: string;
    tokenType: 'Bearer';
    expiresIn: number;
    user: User;
}

export interface Gym {
    id: number;
    name: string;
    slug: string;
    active: boolean;
    usersCount: number;
    createdAt: string;
}

export interface CreateGymRequest {
    name: string;
    slug: string;
    adminEmail: string;
    adminPassword: string;
    adminFullName: string;
}

export interface UpdateGymRequest {
    name: string;
    slug: string;
}

export interface AdminUserRequest {
    email: string;
    password?: string;
    fullName: string;
    role: Role;
    gymId: number | null;
}

export interface Stats {
    totalGyms: number;
    activeGyms: number;
    totalUsers: number;
    /** Socios registrados en todos los gimnasios. */
    totalMembers: number;
    usersByRole: Record<Role, number>;
}

/** Cuerpo de error que devuelve el backend. */
export interface ApiError {
    status: number;
    error: string;
    message: string;
    path: string;
    fieldErrors?: Record<string, string>;
}

// ---------------------------------------------------------------- Gimnasio: planes, socios, membresías

export type DurationUnit = 'DAYS' | 'MONTHS';
export type MembershipStatus = 'ACTIVE' | 'UPCOMING' | 'EXPIRED' | 'CANCELLED' | 'NONE';

export interface Plan {
    id: number;
    name: string;
    description: string | null;
    price: number;
    durationValue: number;
    durationUnit: DurationUnit;
    active: boolean;
    createdAt: string;
}

export interface PlanRequest {
    name: string;
    description: string | null;
    price: number;
    durationValue: number;
    durationUnit: DurationUnit;
}

export interface Member {
    id: number;
    rut: string | null;
    firstName: string;
    lastName: string;
    fullName: string;
    email: string | null;
    phone: string | null;
    birthDate: string | null;
    emergencyContactName: string | null;
    emergencyContactPhone: string | null;
    notes: string | null;
    active: boolean;
    createdAt: string;
    /** Estado de la membresía más relevante del socio. */
    status: MembershipStatus;
    currentPlanName: string | null;
    currentStartDate: string | null;
    currentEndDate: string | null;
    /** Días que le quedan a la membresía vigente (0 = vence hoy). */
    daysLeft: number | null;
    /** Último día cubierto por membresías vigentes o ya pagadas a futuro. */
    paidUntil: string | null;
}

export interface MemberRequest {
    rut: string | null;
    firstName: string;
    lastName: string;
    email: string | null;
    phone: string | null;
    birthDate: string | null;
    emergencyContactName: string | null;
    emergencyContactPhone: string | null;
    notes: string | null;
}

export interface Membership {
    id: number;
    memberId: number;
    planId: number;
    planName: string;
    startDate: string;
    endDate: string;
    pricePaid: number;
    status: MembershipStatus;
    notes: string | null;
    cancelledAt: string | null;
    createdAt: string;
}

export interface MembershipRequest {
    planId: number;
    startDate: string | null;
    pricePaid: number | null;
    notes: string | null;
}

export interface MemberDetail {
    member: Member;
    memberships: Membership[];
}

export interface GymDashboard {
    gymName: string;
    activeMembers: number;
    totalMembers: number;
    expiredLast30Days: number;
    newMembersThisMonth: number;
    salesThisMonth: number;
    revenueThisMonth: number;
    activePlans: number;
    expiringSoon: Member[];
}
