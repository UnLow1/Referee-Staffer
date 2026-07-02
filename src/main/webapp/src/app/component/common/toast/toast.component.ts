import {Component, inject} from '@angular/core';
import {IconComponent} from '../icon/icon.component';
import {ToastService} from '../../../service/toast.service';

/** Renders the single app-wide toast (styles: `.toast` in styles.scss). */
@Component({
  selector: 'app-toast',
  imports: [IconComponent],
  template: `
    @if (toastService.toast(); as toast) {
      <div class="toast" [class.toast--error]="toast.kind === 'error'" role="status">
        <span class="toast__ic">
          <app-icon [name]="toast.kind === 'error' ? 'alert' : 'check'" [size]="14"/>
        </span>
        {{ toast.message }}
      </div>
    }
  `
})
export class ToastComponent {
  readonly toastService = inject(ToastService);
}
