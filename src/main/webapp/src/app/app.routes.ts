import {Routes} from '@angular/router';
import {ShellComponent} from './component/common/shell/shell.component';
import {DashboardComponent} from './component/dashboard/dashboard.component';
import {RefereeListComponent} from './component/referee-list/referee-list.component';
import {RefereeProfileComponent} from './component/referee-profile/referee-profile.component';
import {MatchListComponent} from './component/match-list/match-list.component';
import {MatchDetailComponent} from './component/match-detail/match-detail.component';
import {GradeListComponent} from './component/grade-list/grade-list.component';
import {TeamListComponent} from './component/team-list/team-list.component';
import {StafferComponent} from './component/staffer/staffer.component';
import {ImporterComponent} from './component/importer/importer.component';
import {StandingsComponent} from './component/standings/standings.component';
import {ConfigurationComponent} from './component/configuration/configuration.component';
import {VacationListComponent} from './component/vacation-list/vacation-list.component';

// All app screens live as children of the shell so the sidebar + topbar + main grid is
// rendered once and the router-outlet inside the shell swaps the screen body.
//
// Admin-only routes (teams, standings, vacations) live in the shell's Admin nav group,
// rendered only when the admin section is toggled on (UiSettingsService.adminVisible);
// their direct URLs always resolve.
export const routes: Routes = [
  {
    path: '',
    component: ShellComponent,
    children: [
      {path: '', pathMatch: 'full', redirectTo: 'dashboard'},
      {path: 'dashboard', component: DashboardComponent},
      {path: 'staffer', component: StafferComponent},
      {path: 'matches', component: MatchListComponent},
      {path: 'matches/:id', component: MatchDetailComponent},
      // Legacy form deep-links — the list opens the add/edit drawer on load.
      {path: 'addMatch', component: MatchListComponent},
      {path: 'addMatch/:id', component: MatchListComponent},
      {path: 'referees', component: RefereeListComponent},
      {path: 'referees/:id', component: RefereeProfileComponent},
      // Legacy form deep-links — the list opens the add/edit drawer on load.
      {path: 'addReferee', component: RefereeListComponent},
      {path: 'addReferee/:id', component: RefereeListComponent},
      {path: 'grades', component: GradeListComponent},
      {path: 'import', component: ImporterComponent},
      // Legacy path — the redesign renamed /importer to /import; keep old bookmarks alive.
      {path: 'importer', redirectTo: 'import'},
      {path: 'configuration', component: ConfigurationComponent},
      // Admin (not in nav)
      {path: 'teams', component: TeamListComponent},
      {path: 'addTeam', component: TeamListComponent},
      {path: 'addTeam/:id', component: TeamListComponent},
      {path: 'standings', component: StandingsComponent},
      {path: 'vacations', component: VacationListComponent},
      {path: 'addVacation', component: VacationListComponent},
      {path: 'addVacation/:id', component: VacationListComponent},
      // Must stay last — catches any unknown URL instead of throwing NG04002 to the console.
      {path: '**', redirectTo: 'dashboard'},
    ]
  }
];
