import { Move } from './move';

export interface JoinMatchResponse {
  matchId: string;
  status: 'WAITING_FOR_PLAYER' | 'WAITING_FOR_MOVES' | 'COMPLETED';
  message: string;
}

export interface MultiplayerMatchResponse {
  matchId: string;
  playerOneUsername: string;
  playerTwoUsername: string | null;
  playerOneMove: Move | null;
  playerTwoMove: Move | null;
  status: 'WAITING_FOR_PLAYER' | 'WAITING_FOR_MOVES' | 'COMPLETED';
  result: 'PLAYER_ONE_WIN' | 'PLAYER_TWO_WIN' | 'DRAW' | null;
}

export interface MultiplayerEventResponse {
  type: string;
  matchId: string;
  payload: unknown;
}