import {Component, EventEmitter, Output} from '@angular/core';

@Component({
  selector: 'delete-button',
  templateUrl: './delete-button.component.html',
  styleUrls: ['./delete-button.component.scss']
})
export class DeleteButtonComponent {

  label: string;
  @Output() onClick = new EventEmitter<any>();

  constructor() {
    this.label = 'Delete'
  }

  onClickButton(event) {
    this.onClick.emit(event);
  }
}
