import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { GameApiService } from './game-api.service';

describe('GameApiService', () => {
  let service: GameApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(GameApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('starts a game with a POST to /api/games', () => {
    service.start().subscribe();
    const request = httpMock.expectOne('/api/games');
    expect(request.request.method).toBe('POST');
    request.flush({});
  });

  it('reads a game with a GET to /api/games/:id', () => {
    service.get('game-1').subscribe();
    const request = httpMock.expectOne('/api/games/game-1');
    expect(request.request.method).toBe('GET');
    request.flush({});
  });

  it('refreshes a game with a POST to the refresh endpoint', () => {
    service.refresh('game-1').subscribe();
    const request = httpMock.expectOne('/api/games/game-1/refresh');
    expect(request.request.method).toBe('POST');
    request.flush({});
  });

  it('solves an advertisement with a POST to the solve endpoint', () => {
    service.solve('game-1', 'ad-1').subscribe();
    const request = httpMock.expectOne('/api/games/game-1/ads/ad-1/solve');
    expect(request.request.method).toBe('POST');
    request.flush({});
  });

  it('purchases an item with a POST to the purchase endpoint', () => {
    service.purchase('game-1', 'hpot').subscribe();
    const request = httpMock.expectOne('/api/games/game-1/shop/hpot/purchase');
    expect(request.request.method).toBe('POST');
    request.flush({});
  });

  it('investigates reputation with a POST to the reputation endpoint', () => {
    service.investigate('game-1').subscribe();
    const request = httpMock.expectOne('/api/games/game-1/reputation');
    expect(request.request.method).toBe('POST');
    request.flush({});
  });

  it('runs one automated step with a POST to the auto/step endpoint', () => {
    service.autoStep('game-1').subscribe();
    const request = httpMock.expectOne('/api/games/game-1/auto/step');
    expect(request.request.method).toBe('POST');
    request.flush({});
  });

  it('updates the strategy mode with a PUT carrying the mode in the body', () => {
    service.updateStrategyMode('game-1', 'HIGH_RISK').subscribe();
    const request = httpMock.expectOne('/api/games/game-1/strategy-mode');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ mode: 'HIGH_RISK' });
    request.flush({});
  });
});
