import {Component, EventEmitter, Output} from '@angular/core';

@Component({
  selector: 'edit-button',
  templateUrl: './edit-button.component.html',
  styleUrls: ['./edit-button.component.scss']
})
export class EditButtonComponent {

  label: string;
  @Output() onClick = new EventEmitter<any>();

  constructor() {
    this.label = 'Edit'
  }

  onClickButton(event) {
    this.onClick.emit(event);
  }
}
