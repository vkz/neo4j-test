(ns user
  (:require [prelude :as dev.prelude]
            [app.main]
            [app.db]
            [app.prelude :as prelude]
            [clojure.edn :as edn]
            [clojure.tools.logging :as log]
            [clojure.tools.namespace.repl :refer [refresh] :as tools.ns]
            [clojure.tools.deps.alpha :as tools.deps]))

(defn dirs-allowed-to-refresh
  "Classpath trimmed of dirs under :do-not-refresh key in :dev alias in
  deps.edn"
  []
  (let [do-not-refresh (->> (dev.prelude/deps-edn)
                            :custom :do-not-refresh
                            (into #{}))]
    (->> (dev.prelude/classpath)
         ;; turn them into path strings
         (map str)
         ;; filter out do-not-refresh dirs
         (filter (complement do-not-refresh)))))

;; Set dirs allowed to be refreshed (also effects cider-ns-refresh)
(apply tools.ns/set-refresh-dirs (dirs-allowed-to-refresh))

;;* System restart

(defonce system (atom nil))

(def components
  {:server true})

(defn start []
  (app.db/hydrate!)
  (swap! system (constantly
                 (log/spy
                  (app.main/system components)))))

(defn stop []
  (reset! app.db/*connection nil)
  (some-> @system :server .stop)
  (swap! system (constantly nil)))

(defn restart []
  (println "[user] refresh and restart ...")
  (stop)
  ;; TODO refresh fails if ever you remove or rename a clj file. Cause I think it caches that module
  ;; and attempts to reload it from path that no longer exists. This is rare but with our CGI
  ;; modules coming and going it happens on occasion.
  (tools.ns/refresh :after 'user/start))
