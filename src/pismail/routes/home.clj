(ns pismail.routes.home
  (:require [pismail.layout :as layout]
            [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :refer [ok]]
            [pismail.db.ops :refer [read-msgs]]
            [clojure.java.io :as io]))

(defn home-page []
  (layout/render "home.html" {:deneme "1"}))


(defn mailbox-page [mbox]
  (if (clojure.string/blank? mbox)
    (ring.util.response/redirect "/")
    (do
      ;; do validation
      ;; do ip protection

      (let [msgs (read-msgs mbox)]
        (layout/render "mailbox.html" {:msgs msgs :mbox mbox})))))


(defn mail-page [mbox m]
  (if (or (clojure.string/blank? mbox) (clojure.string/blank? m))
    (ring.util.response/redirect "/")
    (do
      ;; do validation
      ;; do ip protection

      (let [msgs (read-msgs mbox)]
        (layout/render "mail.html" {:msg (nth msgs (Integer. m)) :mbox mbox :m m})))))


(defn- append-script [msg]

  (str
   "<script> window.onload = function() { var height = document.body.scrollWidth); window.parent.document.getElementById('mail-icontent').style.height = height; } </script>"
   msg))


(defn mail-content [mbox m]
  (let [msgs (read-msgs mbox)
        body (:body (nth msgs (Integer. m)))]
    (if-let [html-msg (filter #(.startsWith (:content-type %) "text/html") body)]
      (append-script (:body (first html-msg)))
      (if-let [plain-msg (filter #(.startsWith (:content-type %) "text/plain") body)]
        (append-script (:body (first plain-msg)))
        (if-let [wtf-msg (clojure.string/replace (second (second body)) #"\\\"" "\"")]
          (append-script wtf-msg)
          "Unable to parse message")))))

(defn logout []
  "Logged out")


(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/mailbox" [mbox] (mailbox-page mbox))
  (GET "/logout" [] (logout))
  (GET "/content" [mbox m] (mail-content mbox m))
  (GET "/mail" [mbox m] (mail-page mbox m)))
