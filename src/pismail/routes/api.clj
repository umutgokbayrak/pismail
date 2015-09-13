(ns pismail.routes.api
  (:require [pismail.layout :as layout]
            [compojure.core :refer [defroutes GET POST]]
            [ring.util.response :refer [response]]
            [pismail.db.ops :refer [read-msgs]]
            [pismail.util :as util]
            [clojure.java.io :as io]))


(defn mailbox-api [mbox]
  (if (not (clojure.string/blank? mbox))
    (let [msgs (read-msgs mbox)]
      {:body {:msgs
              (map-indexed
               (fn [idx itm] {idx itm})
               (map #(apply dissoc % [:to :date-recieved :multipart? :content-type :body])
                    msgs))
              :return {:code 0}}})
    {:body {:msgs nil
            :return {:code 1
                     :err "Please select a mailbox to display"}}}))


(defn mail-api [mbox m]
  (if (or (clojure.string/blank? mbox) (clojure.string/blank? m))
    {:body {:msg nil
            :return {:code 1
                     :err "Please select a mailbox to display"}}}
    (try
      (let [msgs (read-msgs mbox)
            msg  (nth msgs (Integer. m))
            body (util/mail-parser (nth msgs (Integer. m)))]
      (response body))
      (catch Exception e
        {:body
         {:msg nil
          :return
          {:code 2
           :err "This message was expunged automatically. It exists no more."}}}))))

(defn config-api []
  {:body {:minVersion 1
          :redirect {:ios ""
                     :android ""}
          :message ""}})


(defroutes api-routes
  (POST "/config" [] (config-api))
  (POST "/mailbox" [mbox] (mailbox-api mbox))
  (POST "/mail" [mbox id] (mail-api mbox id)))
