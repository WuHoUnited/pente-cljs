(ns server.server
  (:require [org.httpkit.server :as hs]))

(defonce server (atom nil))

(defn handler [req]
  (hs/with-channel req channel              ; get the channel
    ;; communicate with client using method defined above
    (hs/on-close channel (fn [status]
                           (println "channel closed")))
    (if (hs/websocket? channel)
      (println "WebSocket channel")
      (println "HTTP channel"))
    (hs/on-receive channel (fn [data]       ; data received from client
                             ;; An optional param can pass to send!: close-after-send?
                             ;; When unspecified, `close-after-send?` defaults to true for HTTP channels
                             ;; and false for WebSocket.  (send! channel data close-after-send?)
                             (println data)
                             (hs/send! channel data))))) ; data is sent directly to the client

(defn stop-server! []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil)))

(defn main [& args]
  ;; The #' is useful when you want to hot-reload code
  ;; You may want to take a look: https://github.com/clojure/tools.namespace
  ;; and http://http-kit.org/migration.html#reload
  (reset! server (hs/run-server #'handler {:port 8080})))

(stop-server!)

(main)
