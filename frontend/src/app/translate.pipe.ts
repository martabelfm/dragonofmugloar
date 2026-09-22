import { Pipe, PipeTransform } from '@angular/core';
import translations from './i18n/en.json';

export type TranslationKey = keyof typeof translations;

export function translate(key: TranslationKey): string {
  return translations[key];
}

@Pipe({ name: 'translate', standalone: true })
export class TranslatePipe implements PipeTransform {
  transform(key: TranslationKey): string {
    return translate(key);
  }
}
