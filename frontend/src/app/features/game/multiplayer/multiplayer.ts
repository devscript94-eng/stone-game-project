import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { API_BASE_URL } from '../../../core/api/api.config';
import { WebsocketService } from '../../../core/websocket/websocket';
import { JoinMatchResponse, MultiplayerMatchResponse } from '../../../shared/models/multiplayer';
import { Move } from '../../../shared/models/move';

@Component({
  selector: 'app-multiplayer',
  standalone: true,
  imports: [],
  templateUrl: './multiplayer.html',
  styleUrl: './multiplayer.scss',
})
export class MultiplayerComponent implements OnInit, OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly websocketService = inject(WebsocketService);

  private userSubscription?: Subscription;
  private matchSubscription?: Subscription;

  matchId: string | null = null;
  message = '';
  matchState: MultiplayerMatchResponse | null = null;

  ngOnInit(): void {
    this.websocketService.connect();
  }

  join(): void {
    this.http.post<JoinMatchResponse>(`${API_BASE_URL}/multiplayer/join`, {}).subscribe(response => {
      this.matchId = response.matchId;
      this.message = response.message;
      this.subscribeToUserQueue();
    });
  }

  submitMove(move: Move): void {
    if (!this.matchId) {
      return;
    }

    this.http.post<MultiplayerMatchResponse>(`${API_BASE_URL}/multiplayer/${this.matchId}/move`, { move })
      .subscribe(response => {
        this.matchState = response;
      });
  }

  private subscribeToUserQueue(): void {
    if (this.userSubscription) {
      return;
    }

    this.userSubscription = this.websocketService
      .watch('/user/queue/match-updates')
      .subscribe(message => {
        const event = JSON.parse(message.body);

        if (event.type === 'MATCH_FOUND') {
          this.matchId = event.matchId;
          this.message = 'Match found';
          this.subscribeToMatchTopic(event.matchId);
        }

        if (event.type === 'WAITING_FOR_PLAYER') {
          this.message = 'Waiting for another player';
        }
      });
  }

  private subscribeToMatchTopic(matchId: string): void {
    this.matchSubscription?.unsubscribe();

    this.matchSubscription = this.websocketService
      .watch(`/topic/matches/${matchId}`)
      .subscribe(message => {
        const event = JSON.parse(message.body);
        this.matchState = event.payload as MultiplayerMatchResponse;
      });
  }

  ngOnDestroy(): void {
    this.userSubscription?.unsubscribe();
    this.matchSubscription?.unsubscribe();
    this.websocketService.disconnect();
  }
}
