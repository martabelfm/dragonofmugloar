import { translate, TranslatePipe } from './translate.pipe';

describe('translate', () => {
  it('returns the English copy for a known key', () => {
    expect(translate('appTitle')).toBe('Mugloar Mission Control');
  });
});

describe('TranslatePipe', () => {
  it('delegates to the translate function', () => {
    const pipe = new TranslatePipe();

    expect(pipe.transform('off')).toBe(translate('off'));
  });
});
