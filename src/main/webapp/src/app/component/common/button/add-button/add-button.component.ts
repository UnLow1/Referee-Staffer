import {Component, Input} from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
    selector: 'app-add-button',
    templateUrl: './add-button.component.html',
    styleUrls: ['./add-button.component.scss'],
    imports: [RouterLink]
})
export class AddButtonComponent {

  @Input() label: string;
  @Input() routerLink: string;
}
