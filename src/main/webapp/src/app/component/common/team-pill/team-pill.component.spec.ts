import {ComponentFixture, TestBed} from '@angular/core/testing';
import {TeamPillComponent} from './team-pill.component';
import {Team} from '../../../model/team';

describe('TeamPillComponent', () => {
  const team: Team = {id: 1, name: 'Legia Warszawa', city: 'Warszawa', points: 30, short: 'LEG'};

  let fixture: ComponentFixture<TeamPillComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({imports: [TeamPillComponent]}).compileComponents();
    fixture = TestBed.createComponent(TeamPillComponent);
  });

  function el(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function shortEl(): HTMLElement {
    return el().querySelector('.team-pill__short') as HTMLElement;
  }

  it('renders the backend short code and the full team name', () => {
    fixture.componentRef.setInput('team', team);
    fixture.detectChanges();

    expect(shortEl().textContent?.trim()).toBe('LEG');
    expect(el().querySelector('.team-pill__name')?.textContent).toContain('Legia Warszawa');
  });

  it('derives an uppercase 3-letter code from the name when short is absent', () => {
    fixture.componentRef.setInput('team', {...team, short: undefined});
    fixture.detectChanges();

    expect(shortEl().textContent?.trim()).toBe('LEG');
  });

  it('renders empty without a team', () => {
    fixture.detectChanges();

    expect(shortEl().textContent?.trim()).toBe('');
    expect(el().querySelector('.team-pill__name')?.textContent?.trim()).toBe('');
  });

  it('marks the home side with the dark variant by default', () => {
    fixture.componentRef.setInput('team', team);
    fixture.detectChanges();

    expect(shortEl().classList).toContain('team-pill__short--home');
  });

  it('drops the dark variant for the away side', () => {
    fixture.componentRef.setInput('team', team);
    fixture.componentRef.setInput('side', 'away');
    fixture.detectChanges();

    expect(shortEl().classList).not.toContain('team-pill__short--home');
  });
});
