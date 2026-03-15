import { Component, inject, NgZone, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { API_BASE_URL } from '../../../core/api/api.config';
import { SinglePlayerGameResponse } from '../../../shared/models/single-player-response';
import { Move } from '../../../shared/models/move';

@Component({
  selector: 'app-single-player',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './single-player.html',
  styleUrls: ['./single-player.scss']
})
export class SinglePlayerComponent {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly ngZone = inject(NgZone);
  private readonly cdr = inject(ChangeDetectorRef);

  result: SinglePlayerGameResponse | null = null;
  isPlaying = false;
  errorMessage = '';

  play(move: Move): void {
    if (this.isPlaying) {
      return;
    }

    this.errorMessage = '';
    this.isPlaying = true;

    this.http.post<SinglePlayerGameResponse>(`${API_BASE_URL}/games/single-player/play`, { move })
      .pipe(
        finalize(() => {
          this.ngZone.run(() => {
            this.isPlaying = false;
            this.cdr.detectChanges();
          });
        })
      )
      .subscribe({
        next: (response) => {
          this.ngZone.run(() => {
            this.result = response;
            this.cdr.detectChanges();
          });
        },
        error: (error) => {
          this.ngZone.run(() => {
            console.error('[SP] request failed:', error);
            this.errorMessage = error?.error?.message ?? 'Unable to play the round.';
            this.cdr.detectChanges();
          });
        }
      });
  }

  clearResult(): void {
    this.result = null;
    this.errorMessage = '';
    this.isPlaying = false;
    this.cdr.detectChanges();
  }

  goHome(): void {
    this.router.navigateByUrl('/');
  }
}