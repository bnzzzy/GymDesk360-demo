import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { environment } from '@/environments/environment';

export type DemoAccount = 'admin' | 'staff' | 'super-admin';

/** Indica si esta instancia es la demo pública (lo informa el backend en /api/public/config). */
@Injectable({ providedIn: 'root' })
export class DemoService {
    private readonly http = inject(HttpClient);
    private loaded = false;

    readonly enabled = signal(false);
    readonly repoUrl = environment.repoUrl;

    load(): void {
        if (this.loaded) return;
        this.loaded = true;
        this.http.get<{ demo: boolean }>(`${environment.apiUrl}/api/public/config`).subscribe({
            next: (c) => this.enabled.set(c.demo),
            error: () => this.enabled.set(false)
        });
    }
}
