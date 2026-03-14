import { Injectable } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class WebsocketService {
  private client: Client | null = null;

  connect(): void {
    if (this.client?.active) {
      return;
    }

    this.client = new Client({
      brokerURL: 'ws://localhost:8080/api/ws',
      reconnectDelay: 5000,
      debug: (msg) => console.log('[STOMP]', msg)
    });

    this.client.activate();
  }

  watch(destination: string): Observable<IMessage> {
    return new Observable<IMessage>((subscriber) => {
      const waitForConnection = () => {
        if (this.client?.connected) {
          const subscription = this.client.subscribe(destination, (message) => {
            subscriber.next(message);
          });

          return () => subscription.unsubscribe();
        }

        const timeout = setTimeout(waitForConnection, 200);
        return () => clearTimeout(timeout);
      };

      return waitForConnection();
    });
  }

  disconnect(): void {
    this.client?.deactivate();
    this.client = null;
  }
}