import {Component, Input, ChangeDetectionStrategy} from '@angular/core';

/**
 * Single-path SVG icon, sized to the design's 16/14/24 grid. Paths come from lucide
 * (merged into a single `d` per icon). If we ever outgrow the inline list, swap in
 * `lucide-angular`.
 */
@Component({
  selector: 'app-icon',
  template: `
    <svg [attr.width]="size" [attr.height]="size" viewBox="0 0 24 24" fill="none"
         stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
      <path [attr.d]="path"></path>
    </svg>
  `,
  changeDetection: ChangeDetectionStrategy.Eager,
  styles: [':host { display: inline-flex; align-items: center; justify-content: center; }']
})
export class IconComponent {
  @Input({required: true}) name!: IconName;
  @Input() size = 16;

  get path(): string {
    return ICON_PATHS[this.name] ?? ICON_PATHS.info;
  }
}

const ICON_PATHS = {
  home:       'M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1Z',
  whistle:    'M4 12a6 6 0 1 0 8.5-5.5L20 4l-1 6h-3',
  grid:       'M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z',
  list:       'M4 6h16M4 12h16M4 18h16',
  // Same three-line glyph as `list` (lucide draws them identically) — separate name so
  // the mobile nav toggle reads as "menu" at the call site.
  menu:       'M4 6h16M4 12h16M4 18h16',
  users:      'M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm-7 9a7 7 0 0 1 14 0M16 11a3 3 0 1 0 0-6M21 20a5 5 0 0 0-4-4.9',
  chart:      'M4 20V10M10 20V4M16 20v-7M22 20H2',
  star:       'm12 3 2.5 6h6.5l-5 4.5L17.5 21 12 17l-5.5 4 1.5-7.5-5-4.5h6.5z',
  cog:        'M12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6Zm9 3a9 9 0 0 1-.2 2l2 1.5-2 3.5-2.4-1a9 9 0 0 1-3.5 2L14.5 23h-5l-.4-2.5a9 9 0 0 1-3.5-2L3.2 19l-2-3.5L3.2 14a9 9 0 0 1 0-4L1.2 8.5l2-3.5 2.4 1a9 9 0 0 1 3.5-2L9.5 1.5h5l.4 2.5a9 9 0 0 1 3.5 2l2.4-1 2 3.5L20.8 10c.1.7.2 1.3.2 2Z',
  upload:     'M12 3v13M6 9l6-6 6 6M4 21h16',
  lock:       'M6 11V8a6 6 0 1 1 12 0v3M5 11h14v10H5z',
  unlock:     'M6 11V8a6 6 0 0 1 11.5-2.5M5 11h14v10H5z',
  swap:       'M7 4 3 8l4 4M3 8h14M17 20l4-4-4-4M21 16H7',
  plus:       'M12 5v14M5 12h14',
  edit:       'M4 20h4l11-11-4-4L4 16Z',
  trash:      'M3 6h18M8 6V4h8v2M6 6l1 14h10l1-14',
  x:          'M6 6l12 12M18 6 6 18',
  check:      'M4 12l5 5L20 6',
  info:       'M12 8h.01M11 12h1v5h1M12 3a9 9 0 1 1 0 18 9 9 0 0 1 0-18Z',
  play:       'M6 4l14 8-14 8z',
  arrowRight: 'M5 12h14M13 6l6 6-6 6',
  chevDown:   'M6 9l6 6 6-6',
  download:   'M12 3v13M6 11l6 6 6-6M4 21h16',
  filter:     'M4 5h16l-6 8v6l-4-2v-4z',
  // Theme toggle (lucide sun / moon, subpaths merged into one d)
  sun:        'M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8ZM12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41',
  moon:       'M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z',
  // Admin nav group icons (lucide shield / trophy / calendar, subpaths merged into one d)
  shield:     'M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1 1 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z',
  trophy:     'M6 9H4.5a2.5 2.5 0 0 1 0-5H6M18 9h1.5a2.5 2.5 0 0 0 0-5H18M4 22h16M10 14.66V17c0 .55-.47.98-.97 1.21C7.85 18.75 7 20.24 7 22M14 14.66V17c0 .55.47.98.97 1.21C16.15 18.75 17 20.24 17 22M18 2H6v7a6 6 0 0 0 12 0V2Z',
  calendar:   'M8 2v4M16 2v4M3 10h18M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Z',
  // Error toast (lucide triangle-alert, subpaths merged into one d)
  alert:      'm21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3ZM12 9v4M12 17h.01',
} as const;

export type IconName = keyof typeof ICON_PATHS;
