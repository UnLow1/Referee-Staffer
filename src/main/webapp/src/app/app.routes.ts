import {NgModule} from '@angular/core';
import {Routes, RouterModule} from '@angular/router';
import {RefereeListComponent} from './component/referee-list/referee-list.component';
import {RefereeFormComponent} from './component/referee-form/referee-form.component';
import {MatchListComponent} from "./component/match-list/match-list.component";
import {MatchFormComponent} from "./component/match-form/match-form.component";

const routes: Routes = [
  {path: 'referees', component: RefereeListComponent},
  {path: 'addReferee', component: RefereeFormComponent},
  {path: 'matches', component: MatchListComponent},
  {path: 'addMatch', component: MatchFormComponent}
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
