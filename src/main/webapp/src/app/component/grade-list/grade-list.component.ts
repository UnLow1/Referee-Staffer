import {Component, OnInit, computed, inject, signal} from '@angular/core';
import {Router} from '@angular/router';
import {forkJoin} from 'rxjs';
import {Match} from '../../model/match';
import {Referee} from '../../model/referee';
import {Team} from '../../model/team';
import {Grade, effectiveGradeValue, isSplitGrade} from '../../model/grade';
import {MatchService} from '../../service/match.service';
import {RefereeService} from '../../service/referee.service';
import {TeamService} from '../../service/team.service';
import {GradeService} from '../../service/grade.service';
import {IconComponent} from '../common/icon/icon.component';
import {RefAvatarComponent} from '../common/ref-avatar/ref-avatar.component';
import {MeterComponent} from '../common/meter/meter.component';

interface GradeRow {
  match: Match;
  referee?: Referee;
  home?: Team;
  away?: Team;
  grade: Grade;
}

/**
 * Grades view — every observer grade in the system, joined with the match it scored
 * and the referee being scored.
 *
 * Backend exposes Grade as just (id, value) — there's no "find grades for referee" or
 * "find grade-with-context" endpoint. We fetch matches + grades + referees + teams in
 * one forkJoin and build the rows client-side. RefereeService's win counters could let
 * us also surface per-referee aggregates, but that's deferred.
 */
@Component({
  selector: 'app-grade-list',
  templateUrl: './grade-list.component.html',
  styleUrl: './grade-list.component.scss',
  imports: [IconComponent, RefAvatarComponent, MeterComponent]
})
export class GradeListComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly matchService = inject(MatchService);
  private readonly refereeService = inject(RefereeService);
  private readonly teamService = inject(TeamService);
  private readonly gradeService = inject(GradeService);

  readonly matches = signal<Match[]>([]);
  readonly refereesById = signal<Map<number, Referee>>(new Map());
  readonly teamsById = signal<Map<number, Team>>(new Map());
  readonly gradesById = signal<Map<number, Grade>>(new Map());

  readonly rows = computed<GradeRow[]>(() => {
    const grades = this.gradesById();
    const refs = this.refereesById();
    const teams = this.teamsById();
    return this.matches()
      .filter(m => m.gradeId != null)
      .map<GradeRow>(m => ({
        match: m,
        referee: m.refereeId != null ? refs.get(m.refereeId) : undefined,
        home: teams.get(m.homeTeamId),
        away: teams.get(m.awayTeamId),
        grade: grades.get(m.gradeId)!
      }))
      .filter(r => r.grade != null)
      .sort((a, b) => (b.match.queue ?? 0) - (a.match.queue ?? 0));
  });

  ngOnInit(): void {
    this.matchService.findAll().subscribe(matches => {
      const teamIds = unique(matches.flatMap(m => [m.homeTeamId, m.awayTeamId]));
      const refereeIds = unique(matches.map(m => m.refereeId).filter(notEmpty));
      const gradeIds = unique(matches.map(m => m.gradeId).filter(notEmpty));

      forkJoin({
        teams: teamIds.length > 0 ? this.teamService.findByIds(teamIds) : Promise.resolve([] as Team[]),
        referees: refereeIds.length > 0 ? this.refereeService.findByIds(refereeIds) : Promise.resolve([] as Referee[]),
        grades: gradeIds.length > 0 ? this.gradeService.findByIds(gradeIds) : Promise.resolve([] as Grade[])
      }).subscribe(({teams, referees, grades}) => {
        this.matches.set(matches);
        this.teamsById.set(toMap(teams));
        this.refereesById.set(toMap(referees));
        this.gradesById.set(toMap(grades));
      });
    });
  }

  shortCode(team: Team | undefined): string {
    if (!team) return '';
    return team.short ?? team.name?.slice(0, 3).toUpperCase() ?? '';
  }

  effectiveValue(grade: Grade): number {
    return effectiveGradeValue(grade);
  }

  isSplit(grade: Grade): boolean {
    return isSplitGrade(grade);
  }

  gradeAsMeter(grade: Grade): number {
    return effectiveGradeValue(grade) * 10;
  }

  gradeKind(grade: Grade): 'default' | 'warn' {
    return effectiveGradeValue(grade) >= 7 ? 'default' : 'warn';
  }

  editGrade(row: GradeRow, event: Event): void {
    event.stopPropagation();
    /*
     * No dedicated "edit grade" form yet — grades are managed via the match form
     * (which has the grade input). Route there for now.
     */
    this.router.navigate(['/addMatch', row.match.id]);
  }

  deleteGrade(row: GradeRow, event: Event): void {
    event.stopPropagation();
    this.gradeService.delete(row.grade).subscribe(() => {
      this.matches.update(prev => prev.map(m =>
        m.id === row.match.id ? {...m, gradeId: undefined as unknown as number} : m
      ));
      this.gradesById.update(prev => {
        const next = new Map(prev);
        next.delete(row.grade.id);
        return next;
      });
    });
  }
}

function unique<T>(arr: T[]): T[] {
  return [...new Set(arr)];
}

function notEmpty<T>(v: T | null | undefined): v is T {
  return v != null;
}

function toMap<T extends {id: number}>(items: T[]): Map<number, T> {
  return new Map(items.map(t => [t.id, t]));
}
