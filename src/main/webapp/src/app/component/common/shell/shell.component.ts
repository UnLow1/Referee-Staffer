import {Component} from '@angular/core';
import {RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';
import {IconComponent, IconName} from '../icon/icon.component';

/**
 * Application shell — sidebar + topbar + scrollable main pane with router-outlet.
 * Replaced the legacy header/footer pair in the 2026-06 redesign. All workspace
 * screens are children of this component via the router.
 */
@Component({
  selector: 'app-shell',
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, IconComponent]
})
export class ShellComponent {
  // Three nav groups mirroring the prototype. `count` is left blank for now — wiring
  // it up requires service calls that should arrive together with the screens that
  // already query those endpoints (step 4+).
  readonly workspaceNav: NavItem[] = [
    {path: '/dashboard', label: 'Overview', icon: 'grid'},
    {path: '/staffer', label: 'Staffer', icon: 'whistle'},
  ];

  readonly dataNav: NavItem[] = [
    {path: '/matches', label: 'Matches', icon: 'list'},
    {path: '/referees', label: 'Referees', icon: 'users'},
    {path: '/grades', label: 'Grades', icon: 'star'},
  ];

  readonly adminNav: NavItem[] = [
    {path: '/teams', label: 'Teams', icon: 'shield'},
    {path: '/standings', label: 'Standings', icon: 'trophy'},
    {path: '/vacations', label: 'Vacations', icon: 'calendar'},
  ];

  readonly setupNav: NavItem[] = [
    {path: '/import', label: 'Import data', icon: 'upload'},
    {path: '/configuration', label: 'Configuration', icon: 'cog'},
  ];

  /**
   * Admin nav gate — hidden by default, like the legacy header's `#admin [hidden]`
   * block the README tip refers to. The modern equivalent of flipping `admin.hidden`
   * in DevTools is `localStorage.setItem('admin.hidden', 'false')` + refresh; the
   * admin routes always resolve by URL regardless.
   */
  readonly showAdmin = localStorage.getItem('admin.hidden') === 'false';
}

interface NavItem {
  path: string;
  label: string;
  icon: IconName;
}
