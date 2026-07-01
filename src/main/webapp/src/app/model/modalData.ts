import {IconName} from '../component/common/icon/icon.component';

export type ModalTone = 'danger' | 'warn' | 'accent';

/**
 * Payload for the confirm / info modal (app-confirm-dialog). `header` and `message`
 * predate the redesign; the optional fields configure the redesigned dialog —
 * defaults (Confirm / Cancel / danger / trash) suit the destructive-delete case.
 */
export interface ModalData {
  header: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  tone?: ModalTone;
  icon?: IconName;
}
