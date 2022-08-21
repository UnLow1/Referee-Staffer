import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {RefereeListComponent} from './component/referee-list/referee-list.component';
import {RefereeFormComponent} from './component/referee-form/referee-form.component';
import {MatchListComponent} from "./component/match-list/match-list.component";
import {MatchFormComponent} from "./component/match-form/match-form.component";
import {GradeListComponent} from "./component/grade-list/grade-list.component";
import {TeamListComponent} from "./component/team-list/team-list.component";
import {TeamFormComponent} from "./component/team-form/team-form.component";
import {StafferComponent} from "./component/staffer/staffer.component";
import {ImporterComponent} from "./component/importer/importer.component";
import {StandingsComponent} from "./component/standings/standings.component";
import {ConfigurationComponent} from "./component/configuration/configuration.component";
import {VacationListComponent} from "./component/vacation-list/vacation-list.component";
import {VacationFormComponent} from "./component/vacation-form/vacation-form.component";

const routes: Routes = [
  {path: 'referees', component: RefereeListComponent},
  {path: 'addReferee/:id', component: RefereeFormComponent},
  {path: 'addReferee', component: RefereeFormComponent},
  {path: 'matches', component: MatchListComponent},
  {path: 'addMatch/:id', component: MatchFormComponent},
  {path: 'addMatch', component: MatchFormComponent},
  {path: 'grades', component: GradeListComponent},
  {path: 'teams', component: TeamListComponent},
  {path: 'addTeam/:id', component: TeamFormComponent},
  {path: 'addTeam', component: TeamFormComponent},
  {path: 'staffer', component: StafferComponent},
  {path: 'importer', component: ImporterComponent},
  {path: "standings", component: StandingsComponent},
  {path: "configuration", component: ConfigurationComponent},
  {path: "vacations", component: VacationListComponent},
  {path: "addVacation/:id", component: VacationFormComponent},
  {path: "addVacation", component: VacationFormComponent}
];

@NgModule({
  imports: [RouterModule.forRoot(routes, { relativeLinkResolution: 'legacy' })],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
