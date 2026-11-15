import { Component, inject } from '@angular/core';
import { RouterModule } from '@angular/router';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { DemoService } from './app/core/demo/demo.service';
import { LayoutService } from './app/layout/service/layout.service';

@Component({
    selector: 'app-root',
    standalone: true,
    imports: [RouterModule, ToastModule, ConfirmDialogModule],
    templateUrl: './app.component.html',
    styleUrl: './app.component.scss'
})
export class AppComponent {
    // Se instancia al arrancar para aplicar el tema (oscuro/claro) también en login y páginas de error
    private readonly layout = inject(LayoutService);

    constructor() {
        inject(DemoService).load();
    }
}
