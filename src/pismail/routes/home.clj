(ns pismail.routes.home
  (:require [pismail.layout :as layout]
            [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :refer [ok]]
            [pismail.db.ops :refer [read-msgs]]
            [clojure.java.io :as io]))

(defn home-page []
  (layout/render "home.html" {:deneme "1"}))


(defn mailbox-page [mbox err]
  (if (clojure.string/blank? mbox)
    (ring.util.response/redirect "/")
    (do
      ;; do validation
      ;; do ip protection

      (let [msgs (read-msgs mbox)]
        (layout/render "mailbox.html" {:msgs msgs :mbox mbox :err err})))))


(defn mail-page [mbox m]
  (if (or (clojure.string/blank? mbox) (clojure.string/blank? m))
    (ring.util.response/redirect "/")
    (do
      ;; do validation
      ;; do ip protection

      (try
        (let [msgs (read-msgs mbox)]
          (layout/render "mail.html" {:msg (nth msgs (Integer. m)) :mbox mbox :m m}))
        (catch Exception e (ring.util.response/redirect "/mailbox?err=1"))))))


(defn- append-script [msg]
  (str
   "<script> window.onload = function() { var height = document.body.scrollWidth); window.parent.document.getElementById('mail-icontent').style.height = height; } </script>"
   msg))


(defn- display-content [part]
  (append-script part))


(defn- html-part [body]
  (:body (first (filter #(if-let [content (:content-type %)]
                    (.startsWith content "text/html"))
                 body))))

(defn- plain-part [body]
  (:body (first (filter #(if-let [content (:content-type %)]
                    (.startsWith content "text/plain"))
                 body))))


(defn- single-part [body]
  (:body (first body)))


(defn html-message? [body]
  (some #(if-let [content (:content-type %)]
           (.startsWith content "text/html")) body))

(defn- wtf-message? [body]
  (nil? (:body (first body))))


(defn- wtf-part [body]
  (nth body 3))


(defn mail-content [mbox m]
  (try
    (let [msgs (read-msgs mbox)
          body (flatten (:body (nth msgs (Integer. m))))]
      (if (wtf-message? body)
        (display-content (wtf-part body))
        (if (> (count body) 1)
          (if (html-message? body)
            (display-content (html-part body))
            (display-content (plain-part body)))
          (display-content (single-part body)))))
    (catch Exception e (str "Unable to get message. Message may be deleted or invalid."))))




;;       (if-let [html-msg (filter #(if-let [content (:content-type %)]
;;                                    (.startsWith content "text/html"))
;;                                 body)]
;;         (append-script (:body (first html-msg))))
;;       (if-let [plain-msg (filter #(if-let [content (:content-type %)]
;;                                     (.startsWith content "text/plain"))
;;                                  body)]
;;         (append-script (:body (first plain-msg))))
;;       (if-let [wtf-msg (clojure.string/replace (second (second body)) #"\\\"" "\"")]
;;         (append-script wtf-msg)
;;         (do
;;           (if-let [html-msg (filter #(if-let [content (:content-type %)]
;;                                        (.startsWith content "text/html"))
;;                                     (first body))]
;;             (append-script (:body (first html-msg))))
;;           (if-let [plain-msg (filter #(if-let [content (:content-type %)]
;;                                         (.startsWith content "text/plain"))
;;                                      (first body))]
;;             (append-script (:body (first plain-msg))))
;;           (if-let [wtf-msg (clojure.string/replace (second (second (first body))) #"\\\"" "\"")]
;;             (append-script wtf-msg)
;;             "Unable to get message. Message may be deleted or invalid."))))
;;     (catch Exception e (.printStackTrace e))))


(defn logout []
  (ring.util.response/redirect "/"))


(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/mailbox" [mbox err] (mailbox-page mbox err))
  (GET "/logout" [] (logout))
  (GET "/content" [mbox m] (mail-content mbox m))
  (GET "/mail" [mbox m] (mail-page mbox m)))
