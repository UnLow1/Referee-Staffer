import {Component, HostListener, inject, signal, ChangeDetectionStrategy} from '@angular/core';
import {RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';
import {UiSettingsService} from '../../../service/ui-settings.service';
import {IconComponent, IconName} from '../icon/icon.component';

/**
 * Application shell — sidebar + topbar + scrollable main pane with router-outlet.
 * All workspace screens are children of this component via the router.
 */
@Component({
  selector: 'app-shell',
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, IconComponent]
})
export class ShellComponent {
  /** Theme + admin-visibility + explainer flags; persisted in localStorage. */
  readonly settings = inject(UiSettingsService);

  /**
   * Mobile-only (<768px) off-canvas nav state, toggled by the hamburger in the topbar.
   * On desktop the sidenav is a static grid column and this flag has no visual effect.
   */
  readonly navOpen = signal(false);

  toggleNav(): void {
    this.navOpen.update(open => !open);
  }

  closeNav(): void {
    this.navOpen.set(false);
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.closeNav();
  }

  // Three nav groups. `count` is left blank — wiring it up requires service calls that
  // should arrive together with the screens that already query those endpoints.
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

}

interface NavItem {
  path: string;
  label: string;
  icon: IconName;
}
