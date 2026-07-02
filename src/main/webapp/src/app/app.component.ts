import {Component} from '@angular/core';
import {RouterOutlet} from '@angular/router';
import {ToastComponent} from './component/common/toast/toast.component';

// AppComponent is now just the bootstrap host — the shell (sidebar + topbar + main) lives
// inside ShellComponent, which is the layout for every routed screen. See app.routes.ts.
// The toast lives here (not in the shell) so it survives any future non-shell routes.
@Component({
  selector: 'app-root',
  template: '<router-outlet></router-outlet><app-toast/>',
  imports: [RouterOutlet, ToastComponent]
})
export class AppComponent {}
