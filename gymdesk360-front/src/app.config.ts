import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, LOCALE_ID, provideZonelessChangeDetection } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeEsCl from '@angular/common/locales/es-CL';
import { provideRouter, withComponentInputBinding, withInMemoryScrolling } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
import { providePrimeNG } from 'primeng/config';
import { appRoutes } from './app.routes';
import { authInterceptor } from './app/core/auth/auth.interceptor';
import { MoneyPreset } from './app/core/theme/money-preset';

registerLocaleData(localeEsCl);

export const appConfig: ApplicationConfig = {
    providers: [
        provideRouter(appRoutes, withComponentInputBinding(), withInMemoryScrolling({ anchorScrolling: 'enabled', scrollPositionRestoration: 'enabled' })),
        provideHttpClient(withFetch(), withInterceptors([authInterceptor])),
        provideZonelessChangeDetection(),
        { provide: LOCALE_ID, useValue: 'es-CL' },
        MessageService,
        ConfirmationService,
        providePrimeNG({
            theme: { preset: MoneyPreset, options: { darkModeSelector: '.app-dark' } },
            translation: {
                accept: 'Sí',
                reject: 'No',
                emptyMessage: 'Sin resultados',
                emptyFilterMessage: 'Sin resultados',
                weak: 'Débil',
                medium: 'Media',
                strong: 'Fuerte',
                passwordPrompt: 'Ingresa una contraseña'
            }
        })
    ]
};
