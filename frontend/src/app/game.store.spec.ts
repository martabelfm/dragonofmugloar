import { Advertisement } from './game.models';
import { sortAds } from './game.store';

describe('advertisement ranking', () => {
  it('puts safety before reward', () => {
    const result = sortAds([
      ad('gamble', 999, 'Gamble'),
      ad('safe', 10, 'Piece of cake'),
      ad('likely', 300, 'Quite likely'),
    ]);
    expect(result.map((value) => value.adId)).toEqual(['safe', 'likely', 'gamble']);
  });

  it('puts higher rewards first within the same tier', () => {
    const result = sortAds([ad('small', 10, 'Sure thing'), ad('large', 80, 'Sure thing')]);
    expect(result.map((value) => value.adId)).toEqual(['large', 'small']);
  });
});

function ad(adId: string, reward: number, probability: string): Advertisement {
  return { adId, reward, probability, message: adId, expiresIn: 5, encrypted: null };
}
