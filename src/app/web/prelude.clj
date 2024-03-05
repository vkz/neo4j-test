(ns app.web.prelude
  (:refer-clojure :exclude [require])
  (:require [clojure.java.io    :as io]
            [clojure.string     :as string]
            [clojure.pprint     :as pp]
            [clojure.tools.logging :as log]
            ;; [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
            ;; [ring.util.anti-forgery :as ring.anti-forgery]
            [ring.util.codec    :as ring.codec]
            [ring.util.response :as ring.response]
            [clj-http.client :as http]
            [fullmeta.prelude   :as fmp :refer [make conform expect]]
            [fullmeta.html      :as html]
            [fullmeta.css       :as css]
            [fullmeta.cgi       :as cgi :refer [my their]]
            [app.prelude     :as prelude])
  (:import [java.net URLDecoder URLEncoder]
           [java.security MessageDigest]))

(defn md5 [^String s]
  (let [algorithm (MessageDigest/getInstance "MD5")
        raw (.digest algorithm (.getBytes s))]
    (format "%032x" (BigInteger. 1 raw))))

;; ATM per environment:
;;
;; dev:  nil
;; test: max-age=15
;; prod: max-age=30
;;
;; Cache-Control set on response is honored, but when absent our `disable-cache` middleware takes
;; over and sets it to `no-store` to avoid surprising caching.
(def cache-control (:cache-control (prelude/env)))

;;* Response and HTML

;; (s/def :url.param/style
;;   (s/nilable #{:indexed :array :comma-separated}))

(defn url-encode [unencoded & [multi-param-style]]
  (if (string? unencoded)
    (URLEncoder/encode unencoded "UTF-8")
    (http/generate-query-string unencoded #_content-type nil multi-param-style)))

(comment
  (url-encode {:foo :bar :baz 42})
  (url-encode (into [] {:foo :bar :baz 42}))
  (url-encode "foo=bar")
  ;; comment
  )

(defn url-decode [encoded]
  (URLDecoder/decode encoded "UTF-8"))

;; TODO Turbo's own anti-forgery setup
;; https://turbo.hotwired.dev/handbook/frames#anti-forgery-support-(csrf)
;;
;; (defn anti-forgery-field
;;   ([]
;;    (anti-forgery-field "__anti-forgery-token"))
;;   ([id]
;;    [:input {:type "hidden"  :name "__anti-forgery-token"  :id id  :value (force *anti-forgery-token*)}]))

(defn script [code] [:script {:dangerouslySetInnerHTML {:__html code}}])


(defn head-prelude []
  (list
   [:meta {:http-equiv "Content-Type" :content "text/html; charset=UTF-8"}]
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
   ;; Favicons
   [:link {:rel "shortcut icon" :type "image/x-icon" :href "/img/favicon/favicon.ico"}]
   [:link {:rel "apple-touch-icon" :sizes "180x180" :type "image/png" :href "/img/favicon/apple-touch-icon.png"}]
   [:link {:rel "icon" :sizes "32x32"   :type "image/png" :href "/img/favicon/favicon-32x32.png"}]
   [:link {:rel "icon" :sizes "16x16"   :type "image/png" :href "/img/favicon/favicon-16x16.png"}]

   [:link {:rel "stylesheet" :type "text/css" :href "/styles.css"
           ;; TODO ok to have that prod?
           :data-turbo-track "reload"
           :hash (md5 (slurp (io/resource "public/styles.css")))}]

   ;; NOTE always ensure manifest "src" entries point to correct paths
   [:link {:rel "manifest" :href "/img/favicon/site.webmanifest"}]


   #_(script "var foo = {};")

   ;; hotwired/turbo
   [:script {:src "/dist/turbo/turbo.es2017-esm.js" :type "module"}]

   ;; fomatic-ui
   [:script {:src "https://cdn.jsdelivr.net/npm/jquery@3.7.1/dist/jquery.min.js"}]
   [:link {:rel "stylesheet" :type "text/css" :href "/dist/fomatic-ui/semantic.min.css" }]
   [:script {:src "/dist/fomatic-ui/semantic.min.js"}]

   ;; fontawesome
   [:script {:src "https://kit.fontawesome.com/4b5de4b57a.js" :crossorigin "anonymous"}]

   [:script {:src "/script.js" :type "text/javascript"}]

   ;; TODO Do I actually want this in dev? This only effects page visits and restoration but only
   ;; body. Has no effect on assets e.g. styles and js loaded in head.
   (when (= "dev" (prelude/env "env_type"))
     [:meta {:name "turbo-cache-control" :content "no-cache"}])))

(defn head [& children]
  [:head
   (head-prelude)
   children
   #_[:title title]
   #_(style css)
   #_(script code)])

(defn html-response [component]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (str
          "<!DOCTYPE html>\n"
          (html/render component))})

(defn assert-get
  ([request]
   (assert-get request ""))
  ([request body]
   (cgi/assert
    (-> request :original :request-method (= :get))
    (constantly
     (ring.response/bad-request body)))))

(defn assert-post
  ([request]
   (assert-post request ""))
  ([request body]
   (cgi/assert
    (-> request :original :request-method (= :post))
    (constantly
     (ring.response/bad-request body)))))
