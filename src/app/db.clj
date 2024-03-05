(ns app.db
  "Datascript mock db"
  (:require
   [datascript.core :as d]
   [clojure.java.io :as io]
   [clojure.data.csv :as csv]
   [java-time :as jt]))

(def schema
  {:account/id
   {:db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity}

   :benchmark/version
   { ;; :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   :benchmark/group
   { ;; :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   :benchmark/bench_simple
   { ;; :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   :benchmark/bench_full
   { ;; :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   :benchmark/mode
   { ;; :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   :benchmark/mean
   { ;; :db/valueType   :db.type/float
    :db/cardinality :db.cardinality/one}

   :benchmark/unit
   { ;; :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   :benchmark/timestamp
   { ;; :db/valueType   :db.type/instant
    :db/cardinality :db.cardinality/one}

   :benchmark/datetime
   { ;; :db/valueType   :db.type/instant
    :db/cardinality :db.cardinality/one}})

(defonce *connection (atom nil))

(defn db []
  (or (deref *connection)
      (reset! *connection (d/create-conn schema))))

(defn parse-datetime [^String datetime]
  (jt/local-date-time (jt/formatter "dd/MM/yyyy HH:mm:ss") datetime))

(defn parse-timestamp [^String timestamp]
  (-> timestamp Long/parseLong jt/instant))

(defn parse-mean [^String mean]
  (Double/parseDouble mean))

(comment
  (parse-datetime "04/02/2020 16:18:10")
  (parse-timestamp "1580833090210")
  ;; end comment
  )

(defn benchmark-groups []
  (d/q '[:find [?group ...]
         :where
         [_ :benchmark/bench_simple ?group]]
       @(db)))

(defn benchmarks-for [group]
  (->> group
       (d/q '[:find [(pull ?e [*]) ...]
              :in $ ?group
              :where
              [?e :benchmark/bench_simple ?group]]
            @(db))
       (group-by :benchmark/bench_full)))

(defn arrange-benchmarks-for [group]
  (-> (benchmarks-for group)
      (update-vals (fn [probes]
                     (-> (group-by :benchmark/version probes)
                         (update-vals #(sort-by :benchmark/timestamp %)))))))

#_
(arrange-benchmarks-for "Top.executePlan")

(defn arrange-benchmarks []
  (into {} (for [group (benchmark-groups)]
             [group (arrange-benchmarks-for group)])))
(comment
  (count (arrange-benchmarks))
  (take 2 (arrange-benchmarks))
  ;; end comment
  )

(defn read-csv-and-transact! [conn file-path]
  (with-open [reader (io/reader (io/resource file-path))]
    (let [rows (doall (csv/read-csv reader))
          headers (first rows)
          attribute #(keyword "benchmark" %)
          attributes (map attribute headers)
          data-rows (rest rows)]
      (d/transact!
       conn
       (for [row data-rows]
         (let [data-map (zipmap attributes row)
               transact-map (-> data-map
                                (update :benchmark/datetime parse-datetime)
                                (update :benchmark/timestamp parse-timestamp)
                                (update :benchmark/mean parse-mean))]
           transact-map))))))

(comment
  (reset! *connection nil)
  (read-csv-and-transact! (db) "db/neo4j_data_old.csv")
  ;; end comment
  )

(defn hydrate! []
  (reset! *connection nil)
  (read-csv-and-transact! (db) "db/neo4j_data_old.csv")
  (read-csv-and-transact! (db) "db/neo4j_data_new.csv"))
