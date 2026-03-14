import { Move } from './move';

export interface SinglePlayerGameResponse {
  gameId: string;
  playerMove: Move;
  computerMove: Move;
  result: 'WIN' | 'LOSE' | 'DRAW';
}