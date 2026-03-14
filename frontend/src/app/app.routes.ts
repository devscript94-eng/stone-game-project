import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login';
import { RegisterComponent } from './features/auth/register/register';
import { HomeComponent } from './features/home/home';
import { SinglePlayerComponent } from './features/game/single-player/single-player';
import { MultiplayerComponent } from './features/game/multiplayer/multiplayer';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'single-player', component: SinglePlayerComponent },
  { path: 'multiplayer', component: MultiplayerComponent },
  { path: '**', redirectTo: '' }
];
