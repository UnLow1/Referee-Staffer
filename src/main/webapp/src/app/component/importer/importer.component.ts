import {Component, OnInit} from '@angular/core';
import {ImporterService} from "../../service/importer.service";
import {ImportResponse} from "../../request/importResonse";

@Component({
  selector: 'app-importer',
  templateUrl: './importer.component.html',
  styleUrls: ['./importer.component.scss']
})
export class ImporterComponent implements OnInit {

  fileToUpload: File = null
  importResult: ImportResponse
  numberOfQueuesToImport: number

  constructor(private importerService: ImporterService) {
  }

  ngOnInit() {
  }

  handleFileInput(event) {
    let files = event.target.files
    this.fileToUpload = files.item(0)
  }

  uploadFileToActivity() {
    this.importerService.postFile(this.fileToUpload, this.numberOfQueuesToImport).subscribe(result => this.importResult = result)
  }
}
