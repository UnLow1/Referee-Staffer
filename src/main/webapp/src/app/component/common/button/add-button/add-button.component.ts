import {Component, Input} from '@angular/core';

@Component({
    selector: 'add-button',
    templateUrl: './add-button.component.html',
    styleUrls: ['./add-button.component.scss'],
    standalone: false
})
export class AddButtonComponent {

  @Input() label: string;
  @Input() routerLink: string;
}
