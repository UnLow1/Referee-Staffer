import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ActivatedRoute, convertToParamMap, Router} from '@angular/router';
import {of} from 'rxjs';
import {TeamListComponent} from './team-list.component';
import {TeamService} from '../../service/team.service';
import {MatchService} from '../../service/match.service';
import {Team} from '../../model/team';
import {Match} from '../../model/match';

describe('TeamListComponent', () => {
  let teamService: jasmine.SpyObj<TeamService>;
  let matchService: jasmine.SpyObj<MatchService>;
  let router: jasmine.SpyObj<Router>;

  const standings: Team[] = [
    {id: 1, name: 'Alfa', city: 'Krakow', points: 40},
    {id: 2, name: 'Beta', city: 'Gdansk', points: 20},
    {id: 3, name: 'Gamma', city: 'Krakow', points: 10}
  ];

  const matches = [
    {id: 11, queue: 1, homeTeamId: 1, awayTeamId: 2, homeScore: 2, awayScore: 1},
    {id: 12, queue: 1, homeTeamId: 3, awayTeamId: 1, homeScore: 0, awayScore: 0},
    // No result yet — must not count as played.
    {id: 13, queue: 2, homeTeamId: 2, awayTeamId: 3}
  ] as Match[];

  beforeEach(() => {
    teamService = jasmine.createSpyObj('TeamService', ['getStandings', 'findById', 'delete']);
    matchService = jasmine.createSpyObj('MatchService', ['findAll']);
    router = jasmine.createSpyObj('Router', ['navigate']);

    teamService.getStandings.and.returnValue(of(standings));
    matchService.findAll.and.returnValue(of(matches));
  });

  async function create(path = 'teams', id?: number): Promise<ComponentFixture<TeamListComponent>> {
    await TestBed.configureTestingModule({
      imports: [TeamListComponent],
      providers: [
        {provide: TeamService, useValue: teamService},
        {provide: MatchService, useValue: matchService},
        {provide: Router, useValue: router},
        {
          provide: ActivatedRoute,
          useValue: {snapshot: {url: [{path}], paramMap: convertToParamMap(id ? {id: String(id)} : {})}}
        }
      ]
    }).compileComponents();
    const fixture = TestBed.createComponent(TeamListComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('renders the standings order and derives played from finished matches only', async () => {
    const fixture = await create();
    const component = fixture.componentInstance;

    expect(component.teams().map(t => t.id)).toEqual([1, 2, 3]);
    expect(component.played(standings[0])).toBe(2);
    expect(component.played(standings[1])).toBe(1); // fixture 13 has no score
    expect(component.played(standings[2])).toBe(1);
    expect((fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr').length).toBe(3);
  });

  it('filters by name or city', async () => {
    const component = (await create()).componentInstance;

    component.setSearch('krakow');
    expect(component.visibleTeams().map(t => t.id)).toEqual([1, 3]);

    component.setSearch('beta');
    expect(component.visibleTeams().map(t => t.id)).toEqual([2]);
  });

  it('scales the points bar to the leader', async () => {
    const component = (await create()).componentInstance;

    expect(component.barPct(standings[0])).toBe(100);
    expect(component.barPct(standings[1])).toBe(50);
  });

  it('deletes only after the confirm, naming the team in the guard', async () => {
    const component = (await create()).componentInstance;
    teamService.delete.and.returnValue(of(void 0));

    component.askDelete(standings[1]);
    expect(component.deleteGuard().message).toContain('Beta');
    expect(teamService.delete).not.toHaveBeenCalled();

    component.confirmDelete();

    expect(teamService.delete).toHaveBeenCalledWith(2);
    expect(component.teams().map(t => t.id)).toEqual([1, 3]);
    expect(component.deleteTarget()).toBeNull();
  });

  describe('deep links', () => {
    it('opens an empty drawer for /addTeam', async () => {
      const component = (await create('addTeam')).componentInstance;

      expect(component.formOpen()).toBeTrue();
      expect(component.editingTeam()).toBeNull();
    });

    it('fetches the team and opens the edit drawer for /addTeam/:id', async () => {
      teamService.findById.and.returnValue(of(standings[0]));

      const component = (await create('addTeam', 1)).componentInstance;

      expect(teamService.findById).toHaveBeenCalledWith(1);
      expect(component.editingTeam()).toEqual(standings[0]);
      expect(component.formOpen()).toBeTrue();
    });

    it('normalizes the URL back to the list on close only when deep-linked', async () => {
      const component = (await create('addTeam')).componentInstance;

      component.closeForm();

      expect(router.navigate).toHaveBeenCalledWith(['/teams']);
    });
  });

  it('reloads the table after a save and closes the drawer', async () => {
    const component = (await create()).componentInstance;
    component.addTeam();
    teamService.getStandings.calls.reset();

    component.onSaved();

    expect(teamService.getStandings).toHaveBeenCalledTimes(1);
    expect(component.formOpen()).toBeFalse();
  });
});
