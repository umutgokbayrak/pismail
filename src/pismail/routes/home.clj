(ns pismail.routes.home
  (:require [pismail.layout :as layout]
            [compojure.core :refer [defroutes GET]]
            [ring.util.response :refer [redirect]]
            [pismail.db.ops :refer [read-msgs]]
            [pismail.util :as util]
            [clojure.java.io :as io]))

(defn home-page []
  (layout/render "home.html"))


(defn mailbox-page [mbox err]
  (if (clojure.string/blank? mbox)
    (redirect "/")
    (do
      ;; do validation
      ;; do ip protection

      (let [msgs (read-msgs mbox)]
        (layout/render "mailbox.html" {:msgs msgs :mbox mbox :err err})))))


(defn mail-page [mbox m]
  (if (or (clojure.string/blank? mbox) (clojure.string/blank? m))
    (redirect "/")
    (do
      ;; do validation
      ;; do ip protection

      (try
        (let [msgs (read-msgs mbox)]
          (layout/render "mail.html" {:msg (nth msgs (Integer. m)) :mbox mbox :m m}))
        (catch Exception e (redirect "/mailbox?err=1"))))))


(defn- append-script [msg]
  (str
   "<script> window.onload = function() { var height = document.body.scrollWidth); window.parent.document.getElementById('mail-icontent').style.height = height; } </script>"
   msg))


(defn- display-content [part]
  (append-script part))


(defn mail-content [mbox m]
  (let [msgs (read-msgs mbox)
        body (util/mail-parser (nth msgs (Integer. m)))]
    (display-content body)))


(defn logout []
  (redirect "/"))


(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/mailbox" [mbox err] (mailbox-page mbox err))
  (GET "/logout" [] (logout))
  (GET "/content" [mbox m] (mail-content mbox m))
  (GET "/mail" [mbox m] (mail-page mbox m)))
