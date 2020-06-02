import {Component, OnInit} from '@angular/core';
import {Config} from "../../model/config";
import {ConfigurationService} from "../../service/configuration.service";

@Component({
  selector: 'app-configuration',
  templateUrl: './configuration.component.html',
  styleUrls: ['./configuration.component.scss']
})
export class ConfigurationComponent implements OnInit {

  config: Config[]

  constructor(private configurationService: ConfigurationService) {
  }

  ngOnInit(): void {
    this.configurationService.findAll().subscribe(config => {
      this.config = config
    })
  }

  onSubmit() {
    this.configurationService.update(this.config).subscribe(config => this.config = config)
  }
}
