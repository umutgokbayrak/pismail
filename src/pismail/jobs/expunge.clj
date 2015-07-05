(ns pismail.jobs.expunge
  (:require [pismail.mail.core :refer [expunge-read]]
            [taoensso.timbre :as timbre]))

(defn init []
  (future
    (while true
     (try
       (let [expunged (expunge-read)]
         (if (> expunged 0)
           (timbre/info "expunged " expunged " msgs")))
       (Thread/sleep 300000)
       (catch Exception e
         (.printStackTrace e)
         (timbre/error e (.getMessage e)))))))


; (init)
