import { Component } from '@angular/core';
import { StatusPage } from '@/app/shared/components/status-page/status-page';

@Component({
    selector: 'app-access',
    standalone: true,
    imports: [StatusPage],
    templateUrl: './access.html',
    styleUrl: './access.scss'
})
export class AccessPage {}
