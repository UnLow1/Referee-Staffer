import {Component} from '@angular/core';
import { HeaderComponent } from './component/common/header/header.component';
import { RouterOutlet } from '@angular/router';
import { FooterComponent } from './component/common/footer/footer.component';
import { AUTHOR } from './config/author.config';

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.css'],
    imports: [HeaderComponent, RouterOutlet, FooterComponent]
})
export class AppComponent {

  readonly title = 'Referee Staffer';
  readonly author = AUTHOR;
  readonly copyright = `© ${new Date().getFullYear()} Copyright: Jamka Solutions`;
}
