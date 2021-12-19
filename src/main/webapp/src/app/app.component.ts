import {Component} from '@angular/core';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {

  title: string;
  facebookUrl: string;
  linkedInUrl: string;
  googleUrl: string;
  steamUrl: string;
  copyright: string;

  constructor() {
    this.title = 'Referee Staffer';
    this.facebookUrl = 'https://www.facebook.com/UnLow1/';
    this.linkedInUrl = 'https://www.linkedin.com/in/adam-jamka-273289145/';
    this.googleUrl = 'mailto: adam.jamka.1995@gmail.com';
    this.steamUrl = 'https://steamcommunity.com/id/UnLow/';
    this.copyright = '© 2021 Copyright: Jamex';
  }
}
