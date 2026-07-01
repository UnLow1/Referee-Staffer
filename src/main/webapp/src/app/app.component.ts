import {Component} from '@angular/core';
import {RouterOutlet} from '@angular/router';

// AppComponent is now just the bootstrap host — the shell (sidebar + topbar + main) lives
// inside ShellComponent, which is the layout for every routed screen. See app.routes.ts.
@Component({
  selector: 'app-root',
  template: '<router-outlet></router-outlet>',
  imports: [RouterOutlet]
})
export class AppComponent {}
