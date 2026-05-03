import {Component} from '@angular/core';
import { HeaderComponent } from './component/common/header/header.component';
import { RouterOutlet } from '@angular/router';
import { FooterComponent } from './component/common/footer/footer.component';

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.css'],
    imports: [HeaderComponent, RouterOutlet, FooterComponent]
})
export class AppComponent {

  title: string;
  facebookUrl: string;
  linkedInUrl: string;
  email: string;
  steamUrl: string;
  copyright: string;

  constructor() {
    this.title = 'Referee Staffer';
    this.facebookUrl = 'https://www.facebook.com/UnLow1/';
    this.linkedInUrl = 'https://www.linkedin.com/in/adam-jamka-273289145/';
    this.email = 'mailto: adam.jamka.1995@gmail.com';
    this.steamUrl = 'https://steamcommunity.com/id/UnLow/';
    this.copyright = '© 2022 Copyright: Jamex';
  }
}
