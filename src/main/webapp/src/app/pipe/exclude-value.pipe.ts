import {Pipe, PipeTransform} from '@angular/core';

@Pipe({ name: 'excludeValue' })
export class ExcludeValuePipe implements PipeTransform {

  transform<T extends { id: number | string }>(value: T[] | null | undefined, args?: number | string): T[] | undefined {
    return value?.filter(item => item.id !== args);
  }
}
