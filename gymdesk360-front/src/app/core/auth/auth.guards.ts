import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Role } from '@/app/core/models/api.models';
import { AuthService } from '@/app/core/auth/auth.service';

/** Exige sesión iniciada. */
export const authGuard: CanActivateFn = (_route, state) => {
    const auth = inject(AuthService);
    return auth.isLoggedIn() ? true : inject(Router).createUrlTree(['/auth/login'], { queryParams: { redirect: state.url } });
};

/** Solo para usuarios sin sesión (login): si ya entró, lo manda a su inicio. */
export const guestGuard: CanActivateFn = () => {
    const auth = inject(AuthService);
    return auth.isLoggedIn() ? inject(Router).createUrlTree([auth.homeFor(auth.user()?.role)]) : true;
};

/** Exige alguno de los roles indicados. */
export const roleGuard =
    (...roles: Role[]): CanActivateFn =>
    () => {
        const auth = inject(AuthService);
        return auth.hasRole(...roles) ? true : inject(Router).createUrlTree(['/auth/access']);
    };

/** Redirige la raíz "/" al inicio que corresponde al rol. */
export const homeRedirectGuard: CanActivateFn = () => {
    const auth = inject(AuthService);
    return inject(Router).createUrlTree([auth.homeFor(auth.user()?.role)]);
};
