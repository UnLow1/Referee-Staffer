import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ActivatedRoute, convertToParamMap, Router} from '@angular/router';
import {of} from 'rxjs';
import {TeamListComponent} from './team-list.component';
import {TeamService} from '../../service/team.service';
import {Standing, Standings} from '../../model/standing';

describe('TeamListComponent', () => {
  let teamService: jasmine.SpyObj<TeamService>;
  let router: jasmine.SpyObj<Router>;

  const rows: Standing[] = [
    {id: 1, name: 'Alfa', city: 'Krakow', points: 40, place: 1, played: 2, wins: 1, draws: 1, losses: 0, goalsFor: 2, goalsAgainst: 1},
    {id: 2, name: 'Beta', city: 'Gdansk', points: 20, place: 2, played: 1, wins: 0, draws: 0, losses: 1, goalsFor: 1, goalsAgainst: 2},
    {id: 3, name: 'Gamma', city: 'Krakow', points: 10, place: 3, played: 1, wins: 0, draws: 1, losses: 0, goalsFor: 0, goalsAgainst: 0}
  ];

  const standings: Standings = {afterQueue: 2, rows};

  beforeEach(() => {
    teamService = jasmine.createSpyObj('TeamService', ['getStandings', 'findById', 'delete']);
    router = jasmine.createSpyObj('Router', ['navigate']);

    teamService.getStandings.and.returnValue(of(standings));
  });

  async function create(path = 'teams', id?: number): Promise<ComponentFixture<TeamListComponent>> {
    await TestBed.configureTestingModule({
      imports: [TeamListComponent],
      providers: [
        {provide: TeamService, useValue: teamService},
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

  it('renders the standings order with the backend-computed played counter', async () => {
    const fixture = await create();
    const component = fixture.componentInstance;

    expect(component.teams().map(t => t.id)).toEqual([1, 2, 3]);
    expect(component.teams().map(t => t.played)).toEqual([2, 1, 1]);
    const rendered = (fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr');
    expect(rendered.length).toBe(3);
    expect(rendered[0].textContent).toContain('2');
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

    expect(component.barPct(rows[0])).toBe(100);
    expect(component.barPct(rows[1])).toBe(50);
  });

  it('deletes only after the confirm, naming the team in the guard', async () => {
    const component = (await create()).componentInstance;
    teamService.delete.and.returnValue(of(void 0));

    component.askDelete(rows[1]);
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
      teamService.findById.and.returnValue(of(rows[0]));

      const component = (await create('addTeam', 1)).componentInstance;

      expect(teamService.findById).toHaveBeenCalledWith(1);
      expect(component.editingTeam()).toEqual(rows[0]);
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
    teamService.getStandings.and.returnValue(of(standings));

    component.onSaved();

    expect(teamService.getStandings).toHaveBeenCalledTimes(1);
    expect(component.formOpen()).toBeFalse();
  });
});
