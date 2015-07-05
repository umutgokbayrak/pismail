(ns pismail.jobs.expire
  (:require [pismail.db.ops :refer [purge-expired]]
            [taoensso.timbre :as timbre]))


(defn init []
  (future
    (while true
     (try
       (timbre/info "expiring redis keys...")

       (purge-expired)
       (Thread/sleep 60000)
       (catch Exception e
         (timbre/error e (.getMessage e)))))))


; (init)
