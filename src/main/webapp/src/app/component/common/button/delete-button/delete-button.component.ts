import {Component, EventEmitter, Output} from '@angular/core';

@Component({
    selector: 'app-delete-button',
    templateUrl: './delete-button.component.html',
    styleUrls: ['./delete-button.component.scss']
})
export class DeleteButtonComponent {

  label = 'Delete';
  @Output() clicked = new EventEmitter<MouseEvent>();

  onClickButton(event: MouseEvent) {
    this.clicked.emit(event);
  }
}
