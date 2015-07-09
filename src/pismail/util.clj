(ns pismail.util)

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


(defn- html-message? [body]
  (some #(if-let [content (:content-type %)]
           (.startsWith content "text/html")) body))

(defn- wtf-message? [body]
  (nil? (:body (first body))))


(defn- wtf-part [body]
  (nth body 3))


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
    (catch Exception e (str "Unable to get message. Message may be deleted or invalid."))))
