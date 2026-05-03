import { Component, OnInit, inject } from '@angular/core';
import {Referee} from "../../model/referee";
import {ActivatedRoute, ParamMap, Router} from "@angular/router";
import {RefereeService} from "../../service/referee.service";
import { FormsModule } from '@angular/forms';

@Component({
    selector: 'app-referee-form',
    templateUrl: './referee-form.component.html',
    styleUrls: ['./referee-form.component.scss'],
    imports: [FormsModule]
})
export class RefereeFormComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private refereeService = inject(RefereeService);


  referee: Referee = new Referee()
  editMode: boolean

  ngOnInit() {
    this.route.paramMap.subscribe(
      (params: ParamMap) => {
        const id = Number(params.get('id'))
        if (id) {
          this.editMode = true
          this.refereeService.findById(id).subscribe(referee => this.referee = referee)
        }
      }
    )
  }

  onSubmit() {
    if (this.editMode)
      this.refereeService.update(this.referee).subscribe(() => this.gotoRefereesList())
    else
      this.refereeService.save(this.referee).subscribe(() => this.gotoRefereesList())
  }

  gotoRefereesList() {
    this.router.navigate(['referees'])
  }
}
