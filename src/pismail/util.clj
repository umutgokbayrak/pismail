(ns pismail.util)

(defn- html-part [body]
  (:body (first (filter #(if-let [content (:content-type %)]
                    (.startsWith content "text/html"))
                 body))))


(defn- plain-part [body]
  (let [body
        (:body
         (first
          (filter
           #(if-let [content (:content-type %)]
              (.startsWith content "text/plain"))
           body)))]
    (clojure.string/replace body #"\n" "<br>")))


(defn- single-part [body]
  (let [body (first body)
        txt (:body body)]
    (if-let [content-type (:content-type body)]
      (do
        (println content-type)
        (if (.startsWith content-type "text/plain")
          (clojure.string/replace txt #"\n" "<br>")
          txt))
      txt)))


(defn- html-message? [body]
  (some #(if-let [content (:content-type %)]
           (.startsWith content "text/html")) body))

(defn- wtf-message? [body]
  (nil? (:body (first body))))


(defn- wtf-part [body]
  (println "wtf-part" body)
  (clojure.string/replace (nth body 3) #"\n" "<br>"))


(defn mail-parser [msg]
  (try
    (let [body (flatten (:body msg))]
      (if (wtf-message? body)
        (wtf-part body)
        (if (> (count body) 1)
          (if (html-message? body)
            (html-part body)
            (plain-part body))
          (single-part body))))
    (catch Exception e
      (str "Unable to get message. Message may be deleted or invalid."))))
