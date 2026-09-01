// `zone.js/testing` (loaded by the unit-test builder through the `testing` build
// configuration) only auto-patches the Jasmine, Mocha and Jest runners. The Vitest
// patch — which runs every `it()` inside a ProxyZone — ships as a separate plugin,
// and without it Angular's `fakeAsync` fails with
// "Expected to be running in 'ProxyZone', but it was not found".
import 'zone.js/plugins/vitest-patch';
