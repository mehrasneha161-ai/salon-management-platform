import { Client, IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

class SocketService {
  private client: Client | null = null
  private subscriptions: Map<string, () => void> = new Map()

  connect(onConnected?: () => void): void {
    this.client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 5000,
      onConnect: () => {
        console.log('WebSocket connected')
        onConnected?.()
      },
      onDisconnect: () => console.log('WebSocket disconnected'),
      onStompError: (frame) => console.error('STOMP error:', frame),
    })
    this.client.activate()
  }

  subscribeToSlots(
    outletId: string,
    date: string,
    callback: (message: string) => void
  ): void {
    if (!this.client?.connected) return
    const destination = `/topic/slots/${outletId}/${date}`
    const sub = this.client.subscribe(destination, (message: IMessage) => {
      callback(message.body)
    })
    this.subscriptions.set(destination, () => sub.unsubscribe())
  }

  unsubscribe(outletId: string, date: string): void {
    const key = `/topic/slots/${outletId}/${date}`
    this.subscriptions.get(key)?.()
    this.subscriptions.delete(key)
  }

  disconnect(): void {
    this.client?.deactivate()
    this.client = null
  }
}

export const socketService = new SocketService()
