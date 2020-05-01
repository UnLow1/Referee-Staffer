import {NgModule} from '@angular/core';
import {Routes, RouterModule} from '@angular/router';
import {RefereeListComponent} from './component/referee-list/referee-list.component';
import {RefereeFormComponent} from './component/referee-form/referee-form.component';
import {MatchesListComponent} from "./component/matches-list/matches-list.component";

const routes: Routes = [
  {path: 'referees', component: RefereeListComponent},
  {path: 'addReferee', component: RefereeFormComponent},
  {path: 'matches', component: MatchesListComponent}
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
