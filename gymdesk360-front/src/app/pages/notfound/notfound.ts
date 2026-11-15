import { Component } from '@angular/core';
import { StatusPage } from '@/app/shared/components/status-page/status-page';

@Component({
    selector: 'app-notfound',
    standalone: true,
    imports: [StatusPage],
    templateUrl: './notfound.html',
    styleUrl: './notfound.scss'
})
export class NotFoundPage {}
