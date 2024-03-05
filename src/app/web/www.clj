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
            [app.prelude           :as prelude]
            [app.db                :as db]
            [app.web.prelude       :as web]))

(defn bench-params-com [benchmark-name]
  (for [param (drop 1 (string/split benchmark-name #"_"))]
    [:div param]))

(defn regression? [[bench {old :mean} {new :mean}]]
  (< old new))

(defn group-regression? [benchmarks]
  ;; TODO proper regression logic should reflect confidence in our means i.e. consider var or var
  ;; coefficient and likely allow for some threshold between old mean and new mean
  (some regression? benchmarks))

(defn tr-com [[bench old new :as benchmark]]
  (let [regression? (regression? benchmark)
        underline " underline decoration-1 underline-offset-4 "
        underline-old (when regression? underline)
        underline-new (when-not regression? underline)
        td-old " whitespace-nowrap px-3 py-4 text-sm text-gray-400 border-solid border-b rounded-none border-gray-200 "
        td-new " whitespace-nowrap px-3 py-4 text-sm text-gray-900 border-solid border-b rounded-none border-gray-200 "
        border " border-solid border-x-2 rounded-none "
        red (when regression? (str border "border-rose-600"))
        green (when-not regression? (str border "border-green-600"))]
    [:tr {:class (or red green)}
     [:td {:class "whitespace-nowrap py-4 pl-4 pr-3 text-sm font-medium text-gray-900 sm:pl-6"} (bench-params-com bench)]
     ;; old readings
     [:td {:class td-old} (->> old :count)]
     [:td {:class td-old} (->> old :var (format "%.2f"))]
     [:td {:class td-old} (->> old :cv (format "%.3f"))]
     [:td {:class (str td-old underline-old)} (->> old :mean (format "%.2f"))]
     ;; new readings
     [:td {:class (str td-new underline-new)} (->> new :mean (format "%.2f"))]
     [:td {:class td-new} (->> new :cv (format "%.3f"))]
     [:td {:class td-new} (->> new :var (format "%.2f"))]
     [:td {:class td-new} (->> new :count)]]))

(defn table-com [benchmarks]
  [:div {:class "mt-8 flow-root"}
   [:div {:class "-mx-4 -my-2 overflow-x-auto sm:-mx-6 lg:-mx-8"}
    [:div {:class "inline-block min-w-full py-2 align-middle sm:px-6 lg:px-8"}
     [:div {:class "overflow-hidden shadow ring-1 ring-black ring-opacity-5 sm:rounded-lg"}
      [:table {:class "min-w-full divide-y divide-gray-300"}
       [:thead {:class "bg-gray-50"}
        [:tr
         [:th {:scope "col" :class "py-3.5 pl-4 pr-3 text-left text-sm font-semibold text-gray-900 sm:pl-6"} "Benchmark"]
         ;; old readings
         [:th {:scope "col" :class "px-3 py-3.5 text-left text-sm font-semibold text-gray-400"} "count"]
         [:th {:scope "col" :class "px-3 py-3.5 text-left text-sm font-semibold text-gray-400"} "var"]
         [:th {:scope "col" :class "px-3 py-3.5 text-left text-sm font-semibold text-gray-400"} "cv"]
         [:th {:scope "col" :class "px-3 py-3.5 text-left text-sm font-semibold text-gray-400"} "mean"]
         ;; new readings
         [:th {:scope "col" :class "px-3 py-3.5 text-left text-sm font-semibold text-gray-900"} "mean"]
         [:th {:scope "col" :class "px-3 py-3.5 text-left text-sm font-semibold text-gray-900"} "cv"]
         [:th {:scope "col" :class "px-3 py-3.5 text-left text-sm font-semibold text-gray-900"} "var"]
         [:th {:scope "col" :class "px-3 py-3.5 text-left text-sm font-semibold text-gray-900"} "count"]]]
       [:tbody
        ;; TODO GO HOME CSS YOU'RE DRUNK divide shadows border settings inside table fml so I had to
        ;; divide manually for each row
        #_{:class "divide-y divide-gray-200 bg-white"}
        {:class "bg-white"}
        ;; render benchmark groups
        (map tr-com benchmarks)]]]]]])

(def toggle-benchmark-state
  {"expanded" "collapsed"
   "collapsed" "expanded"})

(def toggle-benchmark-action
  {"expanded" "Collapse"
   "collapsed" "Expand"})

(defn benchmark-com [group state]
  (let [toggle-state {:group group :state (toggle-benchmark-state state)}
        toggle-action (toggle-benchmark-action state)
        old (db/benchmark-stats group "old")
        new (db/benchmark-stats group "new")
        benchmarks (for [bench (set (concat (keys old) (keys new)))]
                     [bench (get old bench) (get new bench)])
        regression? (group-regression? benchmarks)
        border " border-solid border-x-2 rounded-none "
        red (when regression? (str border "border-rose-600"))
        green (when-not regression? (str border "border-green-600"))]
    [:div {:class (str "bg-gray-100 py-10 rounded-lg" (when (= state "collapsed") (or red green)))}
     [:div {:class "px-4 sm:px-6 lg:px-8 "}
      [:div {:class "sm:flex sm:items-center"}
       [:div {:class "sm:flex-auto"}
        [:h1 {:class "text-base font-semibold leading-6 text-gray-900"} group]
        [:p {:class "mt-2 text-sm text-gray-700"} "TODO render select group benchmark stats here"]]
       [:div {:class "mt-4 sm:ml-16 sm:mt-0 sm:flex-none"}
        [:a {:href (str "/benchmark?" (web/url-encode toggle-state))}
         [:button {:type "button" :class "block rounded-md bg-indigo-600 px-3 py-2 text-center text-sm font-semibold text-white shadow-sm hover:bg-indigo-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600"}
          toggle-action]]]]
      (case state
        ("expanded") (table-com benchmarks)
        ("collapsed") nil)]]))

(defn page [& body]
  (web/html-response
   [:html
    (web/head [:title ""])
    [:body
     [:div {:class "mx-auto max-w-7xl space-y-4 py-8"}
      body]]]))

(defn ^:cgi benchmark [request]
  (let [params (-> request :original :params)
        group (log/spy (:group params))
        state (log/spy (:state params))]
    (page
     [:turbo-frame {:id group}
      (benchmark-com group (or state "collapsed"))])))

(defn ^:cgi cgi [request]
  (web/html-response
   [:html
    (web/head
     [:title "Fullmeta Neo4j"])
    [:body
     [:div {:class "mx-auto max-w-7xl space-y-4 py-8"}
      (for [group (db/benchmark-groups)]
        ;; TODO confirm loading is lazy here (we should probably see network activity as we scroll
        [:turbo-frame
         {:id group
          :src (str "/benchmark?" (web/url-encode {:group group :state "collapsed"}))
          ;; don't load until bench group is scrolled into view
          :loading "lazy"}])]]]))
