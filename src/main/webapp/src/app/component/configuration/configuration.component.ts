import { Component, OnInit, inject } from '@angular/core';
import {Config} from "../../model/config";
import {ConfigurationService} from "../../service/configuration.service";
import { FormsModule } from '@angular/forms';

@Component({
    selector: 'app-configuration',
    templateUrl: './configuration.component.html',
    styleUrls: ['./configuration.component.scss'],
    imports: [FormsModule]
})
export class ConfigurationComponent implements OnInit {
  private configurationService = inject(ConfigurationService);


  config: Config[]

  ngOnInit(): void {
    this.configurationService.findAll().subscribe(config => {
      this.config = config
    })
  }

  onSubmit() {
    this.configurationService.update(this.config).subscribe(config => this.config = config)
  }
}
