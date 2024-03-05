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

(defn table [data]
  [:div {:class "px-4 sm:px-6 lg:px-8"}
   [:div {:class "sm:flex sm:items-center"}
    [:div {:class "sm:flex-auto"}
     [:h1 {:class "text-base font-semibold leading-6 text-gray-900"} "Users"]
     [:p {:class "mt-2 text-sm text-gray-700"} "A list of all the users in your account including their name, title, email and role."]]
    [:div {:class "mt-4 sm:ml-16 sm:mt-0 sm:flex-none"}
     [:button {:type "button", :class "block rounded-md bg-indigo-600 px-3 py-2 text-center text-sm font-semibold text-white shadow-sm hover:bg-indigo-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600"} "Add user"]]]
   [:div {:class "mt-8 flow-root"}
    [:div {:class "-mx-4 -my-2 overflow-x-auto sm:-mx-6 lg:-mx-8"}
     [:div {:class "inline-block min-w-full py-2 align-middle sm:px-6 lg:px-8"}
      [:div {:class "overflow-hidden shadow ring-1 ring-black ring-opacity-5 sm:rounded-lg"}
       [:table {:class "min-w-full divide-y divide-gray-300"}
        [:thead {:class "bg-gray-50"}
         [:tr
          [:th {:scope "col", :class "py-3.5 pl-4 pr-3 text-left text-sm font-semibold text-gray-900 sm:pl-6"} "Name"]
          [:th {:scope "col", :class "px-3 py-3.5 text-left text-sm font-semibold text-gray-900"} "Title"]
          [:th {:scope "col", :class "px-3 py-3.5 text-left text-sm font-semibold text-gray-900"} "Email"]
          [:th {:scope "col", :class "px-3 py-3.5 text-left text-sm font-semibold text-gray-900"} "Role"]
          [:th {:scope "col", :class "relative py-3.5 pl-3 pr-4 sm:pr-6"}
           [:span {:class "sr-only"} "Edit"]]]]
        [:tbody {:class "divide-y divide-gray-200 bg-white"}
         [:tr
          [:td {:class "whitespace-nowrap py-4 pl-4 pr-3 text-sm font-medium text-gray-900 sm:pl-6"} "Lindsay Walton"]
          [:td {:class "whitespace-nowrap px-3 py-4 text-sm text-gray-500"} "Front-end Developer"]
          [:td {:class "whitespace-nowrap px-3 py-4 text-sm text-gray-500"} "lindsay.walton@example.com"]
          [:td {:class "whitespace-nowrap px-3 py-4 text-sm text-gray-500"} "Member"]
          [:td {:class "relative whitespace-nowrap py-4 pl-3 pr-4 text-right text-sm font-medium sm:pr-6"}
           [:a {:href "#", :class "text-indigo-600 hover:text-indigo-900"} "Edit"
            [:span {:class "sr-only"} ", Lindsay Walton"]]]]"<!-- More people... -->" ]]]]]]])

(defn ^:cgi cgi [request]
  (web/html-response
   [:html
    (web/head
     [:title "Fullmeta Neo4j"])

    [:body {:class "mx-auto max-w-xl"}

     #_
     [:script {:src "/script.js" :type "text/javascript"}]]]))
