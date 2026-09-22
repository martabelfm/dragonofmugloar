import { Component, effect, inject } from '@angular/core';
import { GameStore } from '../../game.store';
import { TranslatePipe } from '../../translate.pipe';

@Component({
  imports: [TranslatePipe],
  selector: 'app-mobile-nav',
  styleUrl: './mobile-nav.scss',
  templateUrl: './mobile-nav.html',
})
export class MobileNavComponent {
  protected readonly store = inject(GameStore);

  constructor() {
    effect(() => this.scrollToMobileRecommendation());
  }

  protected scrollToSection(sectionId: 'controls' | 'missions' | 'shop'): void {
    document.getElementById(sectionId)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  protected scrollToTop(): void {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  private scrollToMobileRecommendation(): void {
    const game = this.store.game();
    if (!this.store.guidanceEnabled() || !game || game.finished || game.recommendation.action === 'STOP')
      return;
    setTimeout(() => {
      if (!window.matchMedia('(max-width: 900px)').matches || !this.store.guidanceEnabled()) return;
      document
        .querySelector<HTMLElement>('.recommended, .investigate-recommended')
        ?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    });
  }
}
