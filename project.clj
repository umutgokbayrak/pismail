(defproject pismail "0.1.0-SNAPSHOT"
  :description "Pismail Application"
  :url "http://pismail.com"

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [selmer "0.8.2"]
                 [com.taoensso/timbre "4.0.2"]
                 [com.taoensso/tower "3.0.2"]
                 [com.taoensso/carmine "2.11.1"] ; redis
                 [markdown-clj "0.9.67"]
                 [environ "1.0.0"]
                 [compojure "1.3.4"]
                 [ring/ring-defaults "0.1.5"]
                 [ring/ring-session-timeout "0.1.0"]
                 [ring "1.4.0-RC2" :exclusions [ring/ring-jetty-adapter]]
                 [ring-server "0.4.0"]
                 [cc.qbits/jet "0.6.5"]
                 [metosin/ring-middleware-format "0.6.0"]
                 [metosin/ring-http-response "0.6.2"]
                 [bouncer "0.3.3"]
                 [prone "0.8.2"]
                 [org.clojure/tools.nrepl "0.2.10"]
                 [org.jsoup/jsoup "1.7.3"]
                 [javax.mail/mail "1.4.4"]
                 [org.clojure/data.json "0.2.6"]]
  :min-lein-version "2.0.0"
  :uberjar-name "pismail.jar"
  :jvm-opts ["-server"
             "-Xmx2g"
             "-XX:-OmitStackTraceInFastThrow"
             "-Dorg.eclipse.jetty.util.URI.charset=ISO-8859-9"
             "-Dorg.eclipse.jetty.util.UrlEncoding.charset=ISO-8859-9"
             "-Dorg.eclipse.jetty.server.Request.queryEncoding=ISO-8859-9"]
  ;;enable to start the nREPL server when the application launches
  ;:env {:repl-port 7001}
  :main pismail.core
  :plugins [[lein-ring "0.9.6"]
            [lein-environ "1.0.0"]
            [lein-ancient "0.6.5"]]
  :ring {:handler pismail.handler/app
         :init    pismail.handler/init
         :destroy pismail.handler/destroy
         :uberwar-name "pismail.war"}
  :profiles
  {:uberjar {:omit-source true
             :env {:production true}
             :aot :all}
   :dev {:dependencies [[ring-mock "0.1.5"]
                        [ring/ring-devel "1.3.2"]
                        [pjstadig/humane-test-output "0.7.0"]]
         :repl-options {:init-ns pismail.core}
         :injections [(require 'pjstadig.humane-test-output)
                      (pjstadig.humane-test-output/activate!)]
         :env {:dev true}}})
