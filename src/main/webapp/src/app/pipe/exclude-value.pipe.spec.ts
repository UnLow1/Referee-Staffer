import { ExcludeValuePipe } from './exclude-value.pipe';

describe('ExcludeValuePipe', () => {
  const pipe = new ExcludeValuePipe();
  const items = [
    {id: 1, name: 'first'},
    {id: 2, name: 'second'},
    {id: 3, name: 'third'},
  ];

  it('filters out the item with the given id', () => {
    expect(pipe.transform(items, 2)).toEqual([
      {id: 1, name: 'first'},
      {id: 3, name: 'third'},
    ]);
  });

  it('returns all items when no id matches', () => {
    expect(pipe.transform(items, 99)).toEqual(items);
  });

  it('returns all items when no id is given', () => {
    expect(pipe.transform(items)).toEqual(items);
  });

  it('works with string ids', () => {
    const stringItems = [{id: 'a'}, {id: 'b'}];
    expect(pipe.transform(stringItems, 'a')).toEqual([{id: 'b'}]);
  });

  it('does not exclude on a loosely-equal id of a different type', () => {
    // strict !== on purpose: numeric 1 must not match string '1'
    expect(pipe.transform(items, '1')).toEqual(items);
  });

  it('returns undefined for null or undefined input', () => {
    expect(pipe.transform(null, 1)).toBeUndefined();
    expect(pipe.transform(undefined, 1)).toBeUndefined();
  });
});
