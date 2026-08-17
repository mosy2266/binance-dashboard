import { useEffect, useRef, useState } from 'react'
import { wsMarketUrl } from '../api/marketApi'

const BASE_DELAY_MS = 1000
const MAX_DELAY_MS = 30000

// Mirrors the backend's reconnect policy (exponential backoff, capped at 30s).
export function useMarketSocket(onMessage) {
  const [status, setStatus] = useState('connecting')
  const onMessageRef = useRef(onMessage)

  useEffect(() => {
    onMessageRef.current = onMessage
  }, [onMessage])

  useEffect(() => {
    let socket
    let reconnectTimer
    let attempt = 0
    let closedByCleanup = false

    function connect() {
      setStatus((prev) => (prev === 'open' ? 'reconnecting' : prev))
      socket = new WebSocket(wsMarketUrl())

      socket.onopen = () => {
        attempt = 0
        setStatus('open')
      }

      socket.onmessage = (event) => {
        try {
          onMessageRef.current(JSON.parse(event.data))
        } catch {
          /* ignore malformed message */
        }
      }

      socket.onclose = () => {
        if (closedByCleanup) return
        setStatus('reconnecting')
        attempt += 1
        const delay = Math.min(BASE_DELAY_MS * 2 ** Math.min(attempt, 5), MAX_DELAY_MS)
        reconnectTimer = setTimeout(connect, delay)
      }

      socket.onerror = () => socket.close()
    }

    connect()

    return () => {
      closedByCleanup = true
      clearTimeout(reconnectTimer)
      socket?.close()
    }
  }, [])

  return status
}
