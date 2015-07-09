(ns pismail.db.ops
  (:require [taoensso.timbre :as timbre]
            [clojure.data.json :as json]
            [taoensso.carmine :as car :refer (wcar)])
  (:import  [javax.mail.internet MimeUtility InternetAddress]
            [java.util Date]
            [java.text SimpleDateFormat]))

(def server1-conn {:pool {} :spec {:host "127.0.0.1" :port 6379}})
(defmacro wcar* [& body] `(car/wcar server1-conn ~@body))

(defn evens-and-odds [coll]
  (reduce (fn [result [k v]]
            (update-in result [(if (even? k) :even :odd)] conj v))
          {:even [] :odd []}
          (map-indexed vector coll)))


(defn save-msg [msg]
  (try
    (let [addr (InternetAddress. (:to msg))
          mailbox (first (clojure.string/split (.getAddress addr) #"@"))
          json-str (json/write-str msg)
          date (new Date)
          today (.format (SimpleDateFormat. "yyyy-MM-dd") date)
          this-week (.format (SimpleDateFormat. "yyyy-w") date)
          this-month (.format (SimpleDateFormat. "yyyy-MM") date)
          this-year (.format (SimpleDateFormat. "yyyy") date)]

      ;; ip adreslerini sakla ve yasal sebeplerle log at.

      (wcar* (car/hset (str "mbox-" mailbox) (.getTime date) json-str)
             (car/incr "stats-all")
             (car/hincrby "stats-day" today 1)
             (car/hincrby "stats-week" this-week 1)
             (car/hincrby "stats-month" this-month 1)
             (car/hincrby "stats-year" this-year 1)))
    (catch Exception e
      (.printStackTrace e))))


(defn read-msgs [mailbox]
  (let [msgs-json (wcar* (car/hvals (str "mbox-" mailbox)))]
    (map #(json/read-str % :key-fn keyword) msgs-json)))


(defn purge-expired []
  (let [mailboxes (wcar* (car/keys "mbox-*"))]
    (doseq [mbox mailboxes]

      ;; find and delete the expired email
      (let [dates  (map
                    #(Long. %)
                    (:even (evens-and-odds
                            (wcar* (car/hgetall mbox)))))
            now      (.getTime (new Date))
            expired  (- now 900000)
;            expired  (- now 9000000) ;; TODO: test sonrasinda kaldir
            filtered (filter #(< % expired) dates)]
        (doseq [delkey filtered]
          (wcar* (car/hdel mbox delkey))))

      ; delete empty hashes
      (let [hash-size (wcar* (car/hlen mbox))]
        (if (= 0 hash-size)
          (wcar* (car/del mbox)))))))
