(ns app.main
  (:require [fullmeta.cgi                       :as cgi :refer [my their]]
            [fullmeta.prelude                   :as fm.prelude :refer [make]]
            [app.prelude                       :as prelude]
            [app.web.prelude                   :as web]
            [clojure.java.io                    :as io]
            [clojure.core.async                 :as async]
            [clojure.tools.logging              :as log]
            [clojure.pprint                     :as pp]
            [clojure.string                     :as string]
            [clojure.edn :as edn]
            [ring.adapter.jetty                 :as jetty]
            [ring.middleware.keyword-params     :refer [wrap-keyword-params]]
            [ring.middleware.nested-params      :refer [wrap-nested-params]]
            [ring.middleware.params             :refer [wrap-params]]
            [ring.middleware.json               :refer [wrap-json-params wrap-json-body]]
            [ring.middleware.resource           :refer [wrap-resource]]
            [ring.middleware.content-type       :refer [wrap-content-type]]
            [ring.middleware.default-charset    :refer [wrap-default-charset]]
            [ring.middleware.cookies            :refer [wrap-cookies]]
            [ring.util.response                 :as ring.response]
            [ring.util.request                  :as ring.request])
  (:import [java.lang.management ManagementFactory]
           [org.eclipse.jetty.server.handler StatisticsHandler]
           [org.eclipse.jetty.io ConnectionStatistics]
           [org.eclipse.jetty.jmx MBeanContainer]
           [org.eclipse.jetty.util.log Log]))

(Thread/setDefaultUncaughtExceptionHandler
 (reify Thread$UncaughtExceptionHandler
   (uncaughtException [_ thread ex]
     (log/error ex "Uncaught exception on" (.getName thread)))))

(defn default-handler [request]
  (-> (prelude/env "env_type")
      (case
          "prod"
          (ring.response/not-found (str "HTTP 404: " (:uri request) " not found"))

          "test"
          (ring.response/not-found (str "HTTP 404: " (:uri request) " not found"))

          "dev"
          (ring.response/not-found (str "HTTP 404: " (:uri request) " not found"
                                        (with-out-str
                                          (newline)
                                          (println "-----------")
                                          (pp/pprint request)))))
      (ring.response/content-type "text/plain")))

(defn wrap-health-check [handler]
  (fn health-check [request]
    (if (= (:uri request) "/health")
      (-> {:status 200
           :body "ok"}
          (ring.response/content-type "text/plain"))
      (handler request))))

(comment
  (.addShutdownHook
   (Runtime/getRuntime)
   (Thread. ^Runnable f))
  ;; comment
  )

(defn disable-cache [handler]
  (fn [request]
    (let [response (handler request)]
      (if (ring.response/response? response)
        (if (-> response :headers (contains? "Cache-Control"))
          ;; keep endpoint returned Cache-Control
          response
          ;; endpoint has no Cache-Control, disable cache
          (ring.response/header response "Cache-Control" "no-store"))
        response))))

(defn catch-exceptions [handler]
  (fn [request]
    (try
      (handler request)
      (catch Throwable e
        (let [{:keys [request-method server-name uri query-params headers]} (:original request)]
          (log/error e (format "Exception while handling: %s %s%s with query [%s]. Refered from: %s"
                               (request :request-method)
                               (request :server-name)
                               (request :uri)
                               (request :query-params)
                               (get-in request [:headers "referer"]))))
        
        (ring.response/status 500)))))

(defn configure-jetty-server [server]
  ;; https://www.eclipse.org/jetty/documentation/jetty-9/index.html#jmx-chapter
  ;; https://www.eclipse.org/jetty/documentation/jetty-9/index.html#statistics-handler
  ;; https://www.eclipse.org/jetty/documentation/jetty-10/programming-guide/index.html#pg-server-http-handler-use-util-stats-handler

  ;; These will be available in e.g. MBean Browser in JMC. Faster to use the search bar and look for
  ;; e.g. "stati" or "conne". Pretty damn cool stats! IMO ConnectionStatistics is particularly
  ;; useful cause its coarser grain, while StatisticsHandler deals with every request, guessing
  ;; counts even static resources - not what we care about.

  ;; https://www.eclipse.org/jetty/documentation/jetty-9/index.html#statistics-handler
  ;; see Connection Statistics subsection
  ;; https://www.eclipse.org/jetty/javadoc/jetty-9/org/eclipse/jetty/io/ConnectionStatistics.html
  (doseq [connector (.getConnectors server)]
    (.addBean connector (new ConnectionStatistics)))

  (doto server
    ;; setup jetty JMX support
    (.addBean (new MBeanContainer (ManagementFactory/getPlatformMBeanServer)))
    ;; export jetty loggers as MBeans
    (.addBean (Log/getLog))
    ;; wrap ring handler to expose jetty statistics
    ;; https://www.eclipse.org/jetty/javadoc/jetty-9/org/eclipse/jetty/server/handler/HandlerWrapper.html#insertHandler(org.eclipse.jetty.server.handler.HandlerWrapper)
    ;; equivalent to doing this:
    ;; (.setHandler (doto (new StatisticsHandler) (.setHandler (.getHandler server))))
    (.insertHandler (new StatisticsHandler))))

(defn write-pid [file-path]
  ;; (System/getProperty "user.home")
  (spit file-path (str (.pid (java.lang.ProcessHandle/current)))))

(defn system [{:keys [server] :as opts}]
  (let [bool (fn bool [v]
               (case v
                 (true :on :start :enable :enabled) true
                 (false :off :stop :disable :disabled nil) false
                 v))
        server (bool server)]

    (when-let [pidfile (prelude/env "env_pidfile")]
      (write-pid pidfile))

    ;; Reload routes on every request?
    (add-watch (var cgi/*reload-routes*) :watch-routes (fn reloading? [key v old new] (log/info (str "cgi/*reload-routes* was " (boolean old) ", changed to " (boolean new)))))
    (alter-var-root (var cgi/*reload-routes*)
                    (constantly (case (prelude/env "env_type")
                                  "test" false
                                  "prod" false
                                  "dev" true)))

    (log/info (format "system: starting with options [%s]" opts))
    (log/info (format "env: type [%s], secrets [%s], port [%s], domain [%s]" (prelude/env "env_type") (prelude/env "env_edn") (prelude/env "env_port") (:host (prelude/env))))
    
    {:server (when server
               (jetty/run-jetty
                (-> default-handler
                    (wrap-health-check)
                    ((cgi/middleware {:root "app/web/www"}))
                    (wrap-keyword-params)
                    (wrap-nested-params)
                    (wrap-params)
                    (wrap-json-body {:keywords? true})
                    ;; e.g. part of cookie map {:value "foo" :secure (prelude/prod?) :same-site :lax}
                    (wrap-cookies)
                    (wrap-resource "public" {:allow-symlinks? true})
                    (wrap-content-type {"js" "text/javascript"})
                    (wrap-default-charset "utf-8")
                    (disable-cache)
                    (catch-exceptions))

                {:port (some-> (prelude/env "env_port") parse-long)
                 :allow-null-path-info true
                 :join? false
                 :configurator configure-jetty-server}))}))

(defn -main [& {:as args}]
  ;; accept: clojure -M:dev :server :start
  ;; accept: clojure -M:dev "{:server :start}"
  ;; accept: clojure -M:dev:jmx :server :start
  (let [args (if (string? args)
               (edn/read-string args)
               (-> args
                   (update-keys edn/read-string)
                   (update-vals edn/read-string)))]
    (log/info (format "main: starting with args [%s]" args))
    (let [system (system args)
          shutdown (fn []
                     (log/info "SIGTERM requested. Shutting down.")
                     (-> system :server .stop))]
      (.addShutdownHook (Runtime/getRuntime) (Thread. ^Runnable shutdown))
      (.join (:server system))
      (log/info "Exit"))))

