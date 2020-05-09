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

const routes: Routes = [
  {path: 'referees', component: RefereeListComponent},
  {path: 'addReferee', component: RefereeFormComponent},
  {path: 'matches', component: MatchListComponent},
  {path: 'addMatch', component: MatchFormComponent},
  {path: 'grades', component: GradeListComponent},
  {path: 'teams', component: TeamListComponent},
  {path: 'addTeam', component: TeamFormComponent},
  {path: 'staffer', component: StafferComponent}
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
