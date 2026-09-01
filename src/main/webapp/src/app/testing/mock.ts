import {vi} from 'vitest';
import type {MockedObject} from 'vitest';

/**
 * Builds a mock exposing only the methods a spec actually stubs.
 *
 * Vitest has no `jasmine.createSpyObj` counterpart: `MockedObject<T>` describes a
 * *fully* mocked type, so a partial object literal never satisfies it. This helper
 * keeps the call sites as short as the Jasmine ones while `keyof T` still catches
 * typos in the method names.
 */
export function createMock<T>(methods: readonly (keyof T)[]): MockedObject<T> {
  const mock: Record<PropertyKey, unknown> = {};
  for (const method of methods) {
    mock[method as PropertyKey] = vi.fn().mockName(String(method));
  }
  return mock as MockedObject<T>;
}
