(ns make
  (:require [prelude           :as dev.prelude]
            [clojure.java.io   :as io]
            [clojure.edn       :as edn]
            [clojure.string    :as string]
            [clojure.pprint    :as pp]))


(def ^:dynamic called-from-command-line? nil)

(defn command-line-args->task [& args]
  (let [[task & args] args]
    (if task
      (if (keyword? task)
        task
        (-> task edn/read-string keyword))
      :default)))

(defmulti make command-line-args->task)

(defmethod make :default [& args]
  (println "[make] nothing to do")
  (when called-from-command-line?
    (System/exit 1)))

(defmethod make :java/compile [task & args]
  (println (apply str "compiling .java files in " (or (seq args) ["src"])))
  (doseq [dir (or (seq args) ["src"])]
    (dev.prelude/javac dir)))

(defn -main [& args]
  (binding [called-from-command-line? true]
    (apply make *command-line-args*)))
