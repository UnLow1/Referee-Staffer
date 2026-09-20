// `zone.js/testing` (loaded by the unit-test builder through the `testing` build
// configuration) only auto-patches the Jasmine, Mocha and Jest runners. The Vitest
// patch — which runs every `it()` inside a ProxyZone — ships as a separate plugin,
// and without it Angular's `fakeAsync` fails with
// "Expected to be running in 'ProxyZone', but it was not found".
import 'zone.js/plugins/vitest-patch';

// Node 26 defines a `localStorage` global that stays undefined unless the process was
// started with `--localstorage-file`, and that definition shadows the one jsdom would
// otherwise expose. Anything reading persisted UI preferences (UiSettingsService) would
// blow up on `undefined.getItem`, so install a minimal in-memory Storage instead.
if (!globalThis.localStorage) {
  const store = new Map<string, string>();
  const storage: Storage = {
    get length(): number {
      return store.size;
    },
    clear: () => store.clear(),
    getItem: (key: string) => (store.has(key) ? store.get(key)! : null),
    key: (index: number) => Array.from(store.keys())[index] ?? null,
    removeItem: (key: string) => {
      store.delete(key);
    },
    setItem: (key: string, value: string) => {
      store.set(key, String(value));
    }
  };
  Object.defineProperty(globalThis, 'localStorage', {value: storage, configurable: true});
}
