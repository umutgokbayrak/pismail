(ns pismail.routes.api
  (:require [compojure.core :refer [defroutes POST]]))


(defn mailbox-api []
  "Mailbox API")


(defn mail-api []
  "Mail API")


(defroutes api-routes
  (POST "/mailbox" [] (mailbox-api))
  (POST "/mail" [] (mail-api)))

