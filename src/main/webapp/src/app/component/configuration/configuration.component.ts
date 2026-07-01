import {Component, OnInit, computed, inject, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {Config} from '../../model/config';
import {ConfigurationService} from '../../service/configuration.service';
import {IconComponent} from '../common/icon/icon.component';

interface ConfigGroup {
  id: 'potential' | 'difficulty' | 'effective';
  title: string;
  description: string;
}

/**
 * Group titles + group-level descriptions are still hardcoded — there's only ever 3 of
 * them and the names are stable. Per-config metadata (which group + per-row description)
 * comes from the backend (Config.getGroup / getDescription).
 */
const GROUPS: ConfigGroup[] = [
  {
    id: 'potential',
    title: 'Referee potential',
    description: "How a referee's strength is computed from observer grades and matches refereed."
  },
  {
    id: 'difficulty',
    title: 'Match difficulty',
    description: 'Increments that capture the pressure of a fixture (closeness, derby, edge matches).'
  },
  {
    id: 'effective',
    title: 'Effective value (fairness)',
    description: 'Penalties that balance assignments across the pool.'
  }
];

@Component({
  selector: 'app-configuration',
  templateUrl: './configuration.component.html',
  styleUrl: './configuration.component.scss',
  imports: [FormsModule, IconComponent]
})
export class ConfigurationComponent implements OnInit {
  private readonly configurationService = inject(ConfigurationService);

  readonly groups = GROUPS;
  /** Mutable working copy bound to inputs. */
  readonly values = signal<Map<string, number>>(new Map());
  /** Snapshot of values from the most recent server fetch — what "Reset" reverts to. */
  private readonly defaults = signal<Map<string, number>>(new Map());
  readonly savedAt = signal<Date | null>(null);
  readonly saving = signal(false);

  /** Raw config rows in the original fetch order. */
  private readonly configs = signal<Config[]>([]);

  readonly groupedConfigs = computed<Map<ConfigGroup['id'], Config[]>>(() => {
    const result = new Map<ConfigGroup['id'], Config[]>();
    for (const g of GROUPS) result.set(g.id, []);
    for (const c of this.configs()) {
      // Backend tells us the group via Config.group; default to 'effective' to keep
      // unknown future keys from disappearing.
      const group = (c.group as ConfigGroup['id']) ?? 'effective';
      const bucket = result.get(group) ?? result.get('effective')!;
      bucket.push(c);
    }
    return result;
  });

  ngOnInit(): void {
    this.configurationService.findAll().subscribe(configs => this.adopt(configs));
  }

  configsFor(groupId: ConfigGroup['id']): Config[] {
    return this.groupedConfigs().get(groupId) ?? [];
  }

  defaultFor(name: string): number | undefined {
    return this.defaults().get(name);
  }

  valueFor(name: string): number | undefined {
    return this.values().get(name);
  }

  setValue(name: string, value: number | string): void {
    const num = typeof value === 'number' ? value : Number(value);
    if (Number.isNaN(num)) return;
    this.values.update(prev => {
      const next = new Map(prev);
      next.set(name, num);
      return next;
    });
    this.savedAt.set(null);
  }

  reset(): void {
    this.values.set(new Map(this.defaults()));
    this.savedAt.set(null);
  }

  save(): void {
    const updates: Config[] = this.configs().map(c => ({
      ...c,
      value: this.values().get(c.name) ?? c.value
    }));
    this.saving.set(true);
    this.configurationService.update(updates).subscribe({
      next: refreshed => {
        this.adopt(refreshed);
        this.savedAt.set(new Date());
        this.saving.set(false);
      },
      error: () => this.saving.set(false)
    });
  }

  formatTime(d: Date): string {
    return d.toLocaleTimeString();
  }

  private adopt(configs: Config[]): void {
    this.configs.set(configs);
    const map = new Map(configs.map(c => [c.name, c.value]));
    this.defaults.set(new Map(map));
    this.values.set(map);
  }
}
