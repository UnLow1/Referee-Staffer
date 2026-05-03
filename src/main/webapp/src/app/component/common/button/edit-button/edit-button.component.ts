import {Component, EventEmitter, Output} from '@angular/core';

@Component({
    selector: 'app-edit-button',
    templateUrl: './edit-button.component.html',
    styleUrls: ['./edit-button.component.scss']
})
export class EditButtonComponent {

  label = 'Edit';
  @Output() clicked = new EventEmitter<MouseEvent>();

  onClickButton(event: MouseEvent) {
    this.clicked.emit(event);
  }
}
