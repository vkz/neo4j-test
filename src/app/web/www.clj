(ns app.web.www
  (:require [clojure.tools.logging :as log]
            [clojure.pprint        :as pp]
            [clojure.string        :as string]
            [ring.util.response    :as ring.response]
            [fullmeta.prelude      :as fmp :refer [make conform expect nilify]]
            [fullmeta.cgi          :as cgi :refer [my]]
            [fullmeta.html         :as html]
            [fullmeta.css          :as css]
            [clj-http.client       :as http]
            [app.prelude      :as prelude]
            [app.web.prelude  :as web]))

(defn ^:cgi cgi [request]
  (web/html-response
   [:html
    (web/head
     [:title "Fullmeta Neo4j"])
    [:body
     "hello sailor"


     #_
     [:script {:src "/script.js" :type "text/javascript"}]]]))
