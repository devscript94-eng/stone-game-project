import { Component, OnDestroy, OnInit, inject, NgZone, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';

import { API_BASE_URL } from '../../../core/api/api.config';
import { AuthService } from '../../../core/auth/auth';
import { WebsocketService } from '../../../core/websocket/websocket';
import { JoinMatchResponse, MultiplayerMatchResponse } from '../../../shared/models/multiplayer';
import { Move } from '../../../shared/models/move';

type MultiplayerViewState = 'idle' | 'waiting' | 'playing' | 'completed';

@Component({
  selector: 'app-multiplayer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './multiplayer.html',
  styleUrls: ['./multiplayer.scss']
})
export class MultiplayerComponent implements OnInit, OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly websocketService = inject(WebsocketService);
  private readonly ngZone = inject(NgZone);
  private readonly cdr = inject(ChangeDetectorRef);

  private matchSubscription?: Subscription;

  viewState: MultiplayerViewState = 'idle';
  isJoining = false;

  currentUsername = '';
  opponentUsername: string | null = null;

  matchId: string | null = null;
  matchState: MultiplayerMatchResponse | null = null;

  myMove: string | null = null;
  opponentMove: string | null = null;
  resultLabel = '';

  ngOnInit(): void {
    this.websocketService.connect();

    this.authService.me().subscribe({
      next: (user) => {
        this.currentUsername = user.username;
      }
    });
  }

  join(): void {
    if (this.isJoining || this.viewState !== 'idle') {
      return;
    }

    this.isJoining = true;
    this.viewState = 'waiting';

    this.http.post<JoinMatchResponse>(`${API_BASE_URL}/multiplayer/join`, {}).subscribe({
      next: (response) => {
        this.matchId = response.matchId;
        this.subscribeToMatchTopic(response.matchId);

        if (response.status === 'WAITING_FOR_MOVES') {
          this.loadMatch(response.matchId);
        }

        this.isJoining = false;
      },
      error: () => {
        this.isJoining = false;
        this.viewState = 'idle';
      }
    });
  }

  submitMove(move: Move): void {
    if (!this.matchId) {
      return;
    }

    this.http.post<MultiplayerMatchResponse>(`${API_BASE_URL}/multiplayer/${this.matchId}/move`, { move })
      .subscribe({
        next: (response) => {
          this.applyMatchState(response);
        }
      });
  }

  playAgain(): void {
    this.matchState = null;
    this.matchId = null;
    this.opponentUsername = null;
    this.myMove = null;
    this.opponentMove = null;
    this.resultLabel = '';
    this.viewState = 'idle';
    this.isJoining = false;
    this.join();
  }

  goHome(): void {
    this.router.navigateByUrl('/');
  }

  private subscribeToMatchTopic(matchId: string): void {
    this.matchSubscription?.unsubscribe();

    this.matchSubscription = this.websocketService
      .watch(`/topic/matches/${matchId}`)
      .subscribe({
        next: (message) => {
          this.ngZone.run(() => {
            const event = JSON.parse(message.body);

            if (event.type === 'MATCH_FOUND') {
              this.applyMatchState(event.payload as MultiplayerMatchResponse);
              this.cdr.detectChanges();
              return;
            }

            if (event.type === 'MOVE_SUBMITTED' || event.type === 'MATCH_COMPLETED') {
              this.applyMatchState(event.payload as MultiplayerMatchResponse);
              this.cdr.detectChanges();
            }
          });
        }
      });
  }

  private loadMatch(matchId: string): void {
    this.http.get<MultiplayerMatchResponse>(`${API_BASE_URL}/multiplayer/${matchId}`)
      .subscribe({
        next: (response) => {
          this.ngZone.run(() => {
            this.applyMatchState(response);
            this.cdr.detectChanges();
          });
        }
      });
  }

  private applyMatchState(match: MultiplayerMatchResponse | null): void {
    if (!match) {
      return;
    }

    this.matchState = match;

    const amIPlayerOne = match.playerOneUsername === this.currentUsername;

    this.opponentUsername = amIPlayerOne
      ? match.playerTwoUsername
      : match.playerOneUsername;

    this.myMove = amIPlayerOne
      ? match.playerOneMove
      : match.playerTwoMove;

    this.opponentMove = amIPlayerOne
      ? match.playerTwoMove
      : match.playerOneMove;

    if (match.status === 'WAITING_FOR_PLAYER') {
      this.viewState = 'waiting';
      return;
    }

    if (match.status === 'WAITING_FOR_MOVES') {
      this.viewState = 'playing';
      return;
    }

    if (match.status === 'COMPLETED') {
      this.viewState = 'completed';
      this.resultLabel = this.buildResultLabel(match, amIPlayerOne);
    }
  }

  private buildResultLabel(match: MultiplayerMatchResponse, amIPlayerOne: boolean): string {
    switch (match.result) {
      case 'DRAW':
        return 'Draw';
      case 'PLAYER_ONE_WIN':
        return amIPlayerOne ? 'You win' : 'You lose';
      case 'PLAYER_TWO_WIN':
        return amIPlayerOne ? 'You lose' : 'You win';
      default:
        return 'Match completed';
    }
  }

  ngOnDestroy(): void {
    this.matchSubscription?.unsubscribe();
    this.websocketService.disconnect();
  }
}