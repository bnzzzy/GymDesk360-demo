import { definePreset } from '@primeuix/themes';
import Aura from '@primeuix/themes/aura';

/**
 * Paleta "Money": negro profundo + oro, inspirada en la estética de Floyd Mayweather.
 *
 *  Negro Money  #0B0B0A  (surface-950)   fondo principal en modo oscuro
 *  Carbón       #171716  (surface-900)   tarjetas / sidebar
 *  Grafito      #262624  (surface-800)   bordes y hover
 *  Oro          #D4AF37  (primary-500)   color de marca
 *  Oro claro    #E5C75F  (primary-300)   hover en oscuro
 *  Oro viejo    #8F6922  (primary-700)   texto dorado sobre fondo claro
 *  Hueso        #F7F7F6  (surface-50)    fondo en modo claro
 */
const gold = {
    50: '#fbf8eb',
    100: '#f6eecb',
    200: '#eedc98',
    300: '#e5c75f',
    400: '#dcb840',
    500: '#d4af37',
    600: '#b48a28',
    700: '#8f6922',
    800: '#765522',
    900: '#654721',
    950: '#3a2610'
};

const ink = {
    0: '#ffffff',
    50: '#f7f7f6',
    100: '#ececea',
    200: '#d6d6d3',
    300: '#b4b4af',
    400: '#8c8c86',
    500: '#6e6e69',
    600: '#575753',
    700: '#3f3f3c',
    800: '#262624',
    900: '#171716',
    950: '#0b0b0a'
};

export const MoneyPreset = definePreset(Aura, {
    semantic: {
        primary: gold,
        colorScheme: {
            light: {
                surface: ink,
                primary: {
                    color: '{primary.600}',
                    contrastColor: '#0b0b0a',
                    hoverColor: '{primary.700}',
                    activeColor: '{primary.800}'
                },
                highlight: {
                    background: '{primary.100}',
                    focusBackground: '{primary.200}',
                    color: '{primary.800}',
                    focusColor: '{primary.900}'
                }
            },
            dark: {
                surface: ink,
                primary: {
                    color: '{primary.500}',
                    contrastColor: '#0b0b0a',
                    hoverColor: '{primary.400}',
                    activeColor: '{primary.300}'
                },
                highlight: {
                    background: 'color-mix(in srgb, {primary.500}, transparent 84%)',
                    focusBackground: 'color-mix(in srgb, {primary.500}, transparent 76%)',
                    color: 'rgba(255,255,255,.87)',
                    focusColor: 'rgba(255,255,255,.87)'
                }
            }
        }
    }
});
