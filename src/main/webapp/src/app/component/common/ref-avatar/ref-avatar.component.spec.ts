import {ComponentFixture, TestBed} from '@angular/core/testing';
import {RefAvatarComponent} from './ref-avatar.component';
import {Referee} from '../../../model/referee';

describe('RefAvatarComponent', () => {
  const referee: Referee = {
    id: 7,
    firstName: 'Szymon',
    lastName: 'Marciniak',
    email: 'szymon.marciniak@example.com',
    experience: 15
  };

  let fixture: ComponentFixture<RefAvatarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({imports: [RefAvatarComponent]}).compileComponents();
    fixture = TestBed.createComponent(RefAvatarComponent);
  });

  function el(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function avatar(): HTMLElement {
    return el().querySelector('.avatar') as HTMLElement;
  }

  it('renders uppercase initials and the full name', () => {
    fixture.componentRef.setInput('referee', referee);
    fixture.detectChanges();

    expect(avatar().textContent?.trim()).toBe('SM');
    expect(el().querySelector('.ref-avatar__name')?.textContent).toContain('Szymon Marciniak');
  });

  it('renders empty without a referee', () => {
    fixture.detectChanges();

    expect(avatar().textContent?.trim()).toBe('');
    expect(el().querySelector('.ref-avatar__name')?.textContent?.trim()).toBe('');
  });

  it('hides the email in the default sm size', () => {
    fixture.componentRef.setInput('referee', referee);
    fixture.detectChanges();

    expect(avatar().classList).not.toContain('avatar--lg');
    expect(el().querySelector('.ref-avatar__email')).toBeNull();
  });

  it('shows the email and the large avatar in the lg size', () => {
    fixture.componentRef.setInput('referee', referee);
    fixture.componentRef.setInput('size', 'lg');
    fixture.detectChanges();

    expect(avatar().classList).toContain('avatar--lg');
    expect(el().querySelector('.ref-avatar__email')?.textContent).toContain('szymon.marciniak@example.com');
  });

  it('skips the email line in lg when the referee has none', () => {
    fixture.componentRef.setInput('referee', {...referee, email: ''});
    fixture.componentRef.setInput('size', 'lg');
    fixture.detectChanges();

    expect(el().querySelector('.ref-avatar__email')).toBeNull();
  });

  it('copes with a partial name', () => {
    fixture.componentRef.setInput('referee', {...referee, lastName: ''});
    fixture.detectChanges();

    expect(avatar().textContent?.trim()).toBe('S');
    expect(el().querySelector('.ref-avatar__name')?.textContent?.trim()).toBe('Szymon');
  });
});
