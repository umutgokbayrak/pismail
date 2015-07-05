(ns pismail.jobs.fetch
  (:require [pismail.mail.core :refer [fetch-unread]]
            [pismail.db.ops :refer [save-msg]]
            [taoensso.timbre :as timbre]))

(defn init []
  (future
    (while true
     (try
       ; (timbre/info "fetching messages")

       (let [messages (fetch-unread)]
         (if (> (count messages) 0)
           (do
             (timbre/info "fetched: " (count messages) "msgs.")
             ; save to redis
             (doseq [msg messages]
               (timbre/info (:to msg) "\n\t" (:subject msg))
               (try
                 (save-msg msg)
                 (catch Exception f
                   (.printStackTrace f)))))
           ;(timbre/info (count messages) "msgs. Pass...")))
           ))
       ; sleep for a while
       (Thread/sleep 3000)
       (catch Exception e
         (.printStackTrace e))))))


; (init)
