import { Component, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_BASE_URL } from '../../../core/api/api.config';
import { SinglePlayerGameResponse } from '../../../shared/models/single-player-response';
import { Move } from '../../../shared/models/move';

@Component({
  selector: 'app-single-player',
  standalone: true,
  imports: [],
  templateUrl: './single-player.html',
  styleUrl: './single-player.scss',
})
export class SinglePlayerComponent {
  private readonly http = inject(HttpClient);

  result: SinglePlayerGameResponse | null = null;

  play(move: Move): void {
    this.http.post<SinglePlayerGameResponse>(`${API_BASE_URL}/games/single-player/play`, { move })
      .subscribe(response => {
        this.result = response;
      });
  }
}
