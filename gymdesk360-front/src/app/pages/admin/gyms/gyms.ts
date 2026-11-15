import { DatePipe } from '@angular/common';
import { Component, inject, input, OnInit, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { AdminApiService, apiErrorMessage } from '@/app/core/api/admin-api.service';
import { Gym } from '@/app/core/models/api.models';
import { FieldError } from '@/app/shared/components/field-error/field-error';
import { slugify } from '@/app/shared/ui-helpers';
import { validateForm } from '@/app/shared/utils/forms';

interface GymForm {
    name: string;
    slug: string;
    adminFullName: string;
    adminEmail: string;
    adminPassword: string;
}

const EMPTY_FORM: GymForm = { name: '', slug: '', adminFullName: '', adminEmail: '', adminPassword: '' };

@Component({
    selector: 'app-gyms',
    standalone: true,
    imports: [FormsModule, RouterModule, DatePipe, ButtonModule, DialogModule, IconFieldModule, InputIconModule, InputTextModule, PasswordModule, TableModule, TagModule, TooltipModule, FieldError],
    templateUrl: './gyms.html',
    styleUrl: './gyms.scss'
})
export class GymsPage implements OnInit {
    private readonly api = inject(AdminApiService);
    private readonly toast = inject(MessageService);
    private readonly confirm = inject(ConfirmationService);

    /** ?nuevo=1 abre el diálogo de creación (atajo desde el panel). */
    readonly nuevo = input<string>();

    readonly gyms = signal<Gym[]>([]);
    readonly loading = signal(true);
    readonly saving = signal(false);
    readonly editing = signal<Gym | null>(null);

    dialogVisible = false;
    slugTouched = false;
    form: GymForm = { ...EMPTY_FORM };

    ngOnInit() {
        this.load();
        if (this.nuevo()) {
            this.openNew();
        }
    }

    load() {
        this.loading.set(true);
        this.api.listGyms().subscribe({
            next: (gyms) => {
                this.gyms.set(gyms);
                this.loading.set(false);
            },
            error: (err) => this.fail(err)
        });
    }

    openNew() {
        this.editing.set(null);
        this.form = { ...EMPTY_FORM };
        this.slugTouched = false;
        this.dialogVisible = true;
    }

    openEdit(g: Gym) {
        this.editing.set(g);
        this.form = { ...EMPTY_FORM, name: g.name, slug: g.slug };
        this.slugTouched = true;
        this.dialogVisible = true;
    }

    onNameChange(name: string) {
        if (!this.slugTouched) {
            this.form.slug = slugify(name);
        }
    }

    save(f: NgForm) {
        if (this.saving() || !validateForm(f, this.toast)) return;
        this.saving.set(true);
        const editing = this.editing();
        const req$ = editing
            ? this.api.updateGym(editing.id, { name: this.form.name, slug: this.form.slug })
            : this.api.createGym({ ...this.form, adminEmail: this.form.adminEmail.trim() });

        req$.subscribe({
            next: (gym) => {
                this.gyms.update((list) => (editing ? list.map((x) => (x.id === gym.id ? gym : x)) : [...list, gym]));
                this.toast.add({ severity: 'success', summary: editing ? 'Gimnasio actualizado' : 'Gimnasio creado', detail: gym.name });
                this.saving.set(false);
                this.dialogVisible = false;
            },
            error: (err) => {
                this.saving.set(false);
                this.fail(err);
            }
        });
    }

    toggleActive(g: Gym) {
        const activate = !g.active;
        this.confirm.confirm({
            header: activate ? 'Activar gimnasio' : 'Desactivar gimnasio',
            message: activate ? `¿Reactivar "${g.name}"? Sus usuarios podrán volver a entrar.` : `¿Desactivar "${g.name}"? Sus ${g.usersCount} usuarios perderán el acceso de inmediato.`,
            icon: activate ? 'pi pi-check-circle' : 'pi pi-exclamation-triangle',
            acceptLabel: activate ? 'Activar' : 'Desactivar',
            rejectLabel: 'Cancelar',
            acceptButtonProps: { severity: activate ? 'success' : 'warn' },
            rejectButtonProps: { severity: 'secondary', text: true },
            accept: () =>
                this.api.setGymActive(g.id, activate).subscribe({
                    next: (gym) => {
                        this.gyms.update((list) => list.map((x) => (x.id === gym.id ? gym : x)));
                        this.toast.add({ severity: 'success', summary: activate ? 'Gimnasio activado' : 'Gimnasio desactivado', detail: gym.name });
                    },
                    error: (err) => this.fail(err)
                })
        });
    }

    confirmDelete(g: Gym) {
        this.confirm.confirm({
            header: 'Eliminar gimnasio',
            message: `Se eliminará "${g.name}" junto con sus ${g.usersCount} usuarios. Esta acción no se puede deshacer.`,
            icon: 'pi pi-trash',
            acceptLabel: 'Eliminar',
            rejectLabel: 'Cancelar',
            acceptButtonProps: { severity: 'danger' },
            rejectButtonProps: { severity: 'secondary', text: true },
            accept: () =>
                this.api.deleteGym(g.id).subscribe({
                    next: () => {
                        this.gyms.update((list) => list.filter((x) => x.id !== g.id));
                        this.toast.add({ severity: 'success', summary: 'Gimnasio eliminado', detail: g.name });
                    },
                    error: (err) => this.fail(err)
                })
        });
    }

    private fail(err: unknown) {
        this.loading.set(false);
        this.toast.add({ severity: 'error', summary: 'Error', detail: apiErrorMessage(err), life: 5000 });
    }
}
