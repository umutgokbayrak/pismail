(ns pismail.mail.core
  (:require [pismail.mail.parser :refer [html->text]]
            [pismail.mail.message :as message]
            [pismail.mail.message :refer [read-message]]
            [pismail.mail.folder :as folder])
  (:import  [java.util Properties]
            [javax.mail.search FlagTerm]
            [java.io FileInputStream File]
            [javax.mail.internet MimeMessage MimeMultipart InternetAddress]
            [javax.mail Session Store Folder Message Flags Flags$Flag]))



(def protocol "imap")
(def server "CHANGEME")
(def username "CHANGEME")
(def password "CHANGEME")


(def imap-folder-names
  {:inbox "INBOX"
   :sent  "/Sent Mail"
   :spam  "/Spam"})

(defonce ^:dynamic *store* nil)

(defmacro with-store
  "Takes a store which has been connected, and binds to to *store* within the
  scope of the form.

  **Usage:**

   user> (with-store (imap-store \"username@domain.com\" \"password\")
           (read-messages :inbox 5))
   ;=> "
  [s & body]
  `(binding [*store* ~s]
     ~@body))


(defn as-properties
  [m]
  (let [p (Properties.)]
    (doseq [[k v] m]
      (.setProperty p (str k) (str v)))
        p))

(defn file->message
  "read a downloaded mail message in the same format
   as you would find on the mail server. This can
   be used to read saved messages from text files
   and for parsing fixtures in tests etc"
  [path-to-message]
  (let [props (Session/getDefaultInstance (Properties.))
        msg (FileInputStream. (File. path-to-message))]
    (MimeMessage. props msg)))

;; Mail store
;; *******************************************************

(defn store
  "A store models a message store and its access protocol,
   for storing and retrieving messages"
  [protocol server email pass]
  (let [p (as-properties
            [["mail.store.protocol" protocol]])]
    (try
      (doto (.getStore (Session/getDefaultInstance p) protocol)
        (.connect server email pass))
      (catch javax.mail.AuthenticationFailedException e
        (format "Invalid credentials %s : %s" email pass)))))

(defn gen-store
  "Generates an email store which allows us access to our inbox"
  [protocol server email password]
    (store protocol server email password))

(defn connected?
  "Returns true if a connection is established"
  [^com.sun.mail.imap.IMAPStore s]
  (.isConnected s))

(defn close-store
  "Close an open IMAP store connection"
  [s]
  (.close s))

(defn get-default-folder
  ^{:doc "Returns a Folder object that represents the 'root' of the default
          namespace presented to the user by the Store."}
  [^com.sun.mail.imap.IMAPStore s]
  (.getDefaultFolder s))

(defn get-folder
  "Return the Folder object corresponding to the given name."
  [^com.sun.mail.imap.IMAPStore s name]
  (.getFolder s name))

;; *********************************************************

(def sub-folder?
  "Check if a folder is a sub folder"
  (fn [folder]
    (if (= 0 (bit-and
               (.getType folder) Folder/HOLDS_FOLDERS))
      false
      true)))

(defn folders
  "Returns a seq of all IMAP folders inlcuding sub folders"
  ([store] (folders store (.getDefaultFolder store)))
  ([store f]
  (map
    #(cons (.getName %)
      (if (sub-folder? %)
        (folders store %)))
          (.list f))))

(def folder-permissions
  {:readonly Folder/READ_ONLY
   :readwrite Folder/READ_WRITE})

(defn open-folder
  "Open a folder. Requires that a folder-name be a valid imap folder
   i.e :inbox :sent :spam etc"
  ([folder-name perm-level] (open-folder *store* folder-name perm-level))
  ([store folder-name perm-level]
     (when-let [folder (get imap-folder-names folder-name)]
       (let [root-folder (.getDefaultFolder store)
             found-folder (get-folder root-folder folder)]
         (doto found-folder
           (.open (get folder-permissions perm-level)))))))

(defn message-count
  "Returns the number of messages in a folder"
  ([folder-name] (message-count *store* folder-name))
  ([store folder-name]
     (let [folder (open-folder store folder-name :readonly)]
       (.getMessageCount folder))))

(defn user-flags [message]
  (let [flags (message/flags message)]
    (.getUserFlags flags)))

(defn unread-messages
  "Find unread messages"
  ([folder-name] (unread-messages *store* folder-name))
  ([^com.sun.mail.imap.IMAPStore store folder-name]
     (let [folder (open-folder store folder-name :readwrite)]
       (.search folder
         (FlagTerm. (Flags. Flags$Flag/SEEN) false)))))


(defn seen-messages
  "Find unread messages"
  ([folder-name] (unread-messages *store* folder-name))
  ([^com.sun.mail.imap.IMAPStore store folder-name]
     (let [folder (open-folder store folder-name :readonly)]
       (.search folder
         (FlagTerm. (Flags. Flags$Flag/SEEN) true)))))


(defn mark-all-read
  "Mark all messages in folder as read"
  ([folder-name] (mark-all-read *store* folder-name))
  ([^com.sun.mail.imap.IMAPStore store folder-name]
     (let [folder (open-folder store folder-name :readwrite)
           messages (.search folder (FlagTerm. (Flags. Flags$Flag/SEEN) false))]
       (doall (map #(.setFlags % (Flags. Flags$Flag/SEEN) true) messages))
       nil)))


(defn mark-all-deleted
  "Mark all messages in folder as deleted"
  ([folder-name] (mark-all-deleted *store* folder-name))
  ([^com.sun.mail.imap.IMAPStore store folder-name]
     (let [folder (open-folder store folder-name :readwrite)
           messages (.search folder (FlagTerm. (Flags. Flags$Flag/DELETED) false))]
       (doall (map #(.setFlags % (Flags. Flags$Flag/DELETED) true) messages))
       (.expunge folder)
       nil)))


(defn save-message-to-file
  [message]
  (let [filename
          (apply str
           (drop 1 (message/id message)))]
    (.writeTo message
      (java.io.FileOutputStream.
        filename))))

(defn dump
  "Handy function that dumps out a batch of emails to disk"
  [msgs]
  (let [message-futures
         (doall
           (map #(future (save-message-to-file %)) msgs))]
    (map deref message-futures)))

;; Public API
;; *********************************************************

(defn all-messages
  "Given a store and folder returns all messages
   reversed so the newest messages come first"
  ([folder-name] (all-messages *store* folder-name))
  ([^com.sun.mail.imap.IMAPStore store folder-name]
     (let [folder (open-folder store folder-name :readonly)]
       (->> (.getMessages folder)
             reverse))))

(defn inbox
  "Get n messages from your inbox"
  ([] (all-messages *store* :inbox))
  ([store] (all-messages store :inbox)))


(defn search-inbox
  "Search your inbox for a specific term
   Returns a vector of IMAPMessage objects"
  [store term]
  (let [inbox (open-folder store :inbox :readonly)]
    (into []
      (folder/search inbox term))))

(defn fetch-unread []
  (let [imap-store (gen-store protocol server username password)
        my-inbox-messages (unread-messages imap-store :inbox)]
    (doall (map #(.setFlags % (Flags. Flags$Flag/SEEN) true) my-inbox-messages))
    (map #(read-message %) my-inbox-messages)))


(defn expunge-read []
  (let [imap-store (gen-store protocol server username password)
        folder (open-folder imap-store :inbox :readwrite)
        seen-messages (.search folder (FlagTerm. (Flags. Flags$Flag/SEEN) true))]
    (doall (map #(.setFlags % (Flags. Flags$Flag/DELETED) true) seen-messages))
    (.expunge folder)
    (.close folder true)
    (close-store imap-store)
    (count seen-messages)))
