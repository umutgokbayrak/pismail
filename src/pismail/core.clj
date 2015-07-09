(ns pismail.core
  (:require [pismail.handler :refer [app init destroy]]
            [qbits.jet.server :refer [run-jetty]]
            [ring.middleware.reload :as reload]
            [taoensso.timbre :as timbre]
            [environ.core :refer [env]]
            [pismail.jobs.fetch :as mail-fetch]
            [pismail.jobs.expunge :as mail-expunge]
            [pismail.jobs.expire :as db-expire]
            )
  (:gen-class))

(defonce server (atom nil))

(defn parse-port [[port]]
  (Integer/parseInt (or port (env :port) "3001")))

(defn start-server [port]
  (init)
  (reset! server
          (run-jetty
            {:ring-handler (if (env :dev) (reload/wrap-reload #'app) app)
             :port port
             :join? false})))

(defn stop-server []
  (when @server
    (destroy)
    (.stop @server)
    (reset! server nil)))

(defn start-app [args]
  (let [port (parse-port args)]

    (timbre/info "initiating mail import loop")
    (mail-fetch/init)

    (timbre/info "initiating mail expunge loop")
    (mail-expunge/init)

    (if (not (env :dev))
      (do
        (timbre/info "initiating db expire loop")
        (db-expire/init)))

    (.addShutdownHook (Runtime/getRuntime) (Thread. stop-server))
    (timbre/info "server is starting on port " port)
    (start-server port)))

(defn -main [& args]
  (start-app args))
