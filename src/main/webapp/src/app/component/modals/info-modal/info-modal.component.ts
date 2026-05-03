import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogTitle, MatDialogContent, MatDialogActions, MatDialogClose } from '@angular/material/dialog';
import {ModalData} from "../../../model/modalData";
import { CdkScrollable } from '@angular/cdk/scrolling';

@Component({
    selector: 'app-info-modal',
    templateUrl: './info-modal.component.html',
    styleUrls: ['./info-modal.component.scss'],
    imports: [MatDialogTitle, CdkScrollable, MatDialogContent, MatDialogActions, MatDialogClose]
})
export class InfoModalComponent {  data = inject<ModalData>(MAT_DIALOG_DATA);

}
