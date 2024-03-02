(ns app.prelude
  (:require [fullmeta.prelude :as fm.prelude]
            [clojure.java.io  :as io]
            [clojure.edn      :as edn]
            [clojure.tools.logging :as log]))

#_
(System/getProperties)

(defn read-env []
  (let [env-edn (or (System/getenv "env_edn")
                    (System/getProperty "env_edn"))
        secrets (some-> env-edn
                        (io/resource)
                        (slurp)
                        (edn/read-string))]
    (or secrets
        (let [msg "Unable to find env secrets given by env_edn System property. Either not specified or resource missing."
              data {'(env "env_edn") env-edn
                    '(io/resource (env "env_edn")) secrets}]
          (log/error (format (str msg " Data: %s") data))
          (throw
           (ex-info msg data))))))

(defonce env* (atom nil))


(defn env
  ([]
   (or (deref env*)
       (reset! env* (read-env))))
  ([s]
   (or (System/getenv s)
       (System/getProperty s)
       (get (env) s))))

(defn domain [& [subdomain]]
  (let [env (env)
        scheme (:scheme env)
        host (:host env)]
    (str scheme "://" subdomain (when subdomain ".") host)))

(defn domain-schemeless [& [subdomain]]
  (let [env (env)
        scheme (:scheme env)
        host (:host env)]
    (str subdomain (when subdomain ".") host)))

(comment
  (reset! env* nil)

  (env)
  ;; => {}
  (env "env_type")
  ;; => "dev"
  (env "env_platform")
  ;; => "local"
  ;; comment
  )
