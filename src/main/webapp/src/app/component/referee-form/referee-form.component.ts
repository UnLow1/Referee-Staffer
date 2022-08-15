import {Component, OnInit} from '@angular/core';
import {Referee} from "../../model/referee";
import {ActivatedRoute, ParamMap, Router} from "@angular/router";
import {RefereeService} from "../../service/referee.service";

@Component({
  selector: 'app-referee-form',
  templateUrl: './referee-form.component.html',
  styleUrls: ['./referee-form.component.scss']
})
export class RefereeFormComponent implements OnInit {

  referee: Referee = new Referee()
  editMode: boolean

  constructor(private route: ActivatedRoute, private router: Router, private refereeService: RefereeService) {
  }

  ngOnInit() {
    this.route.paramMap.subscribe(
      (params: ParamMap) => {
        let id = Number(params.get('id'))
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
