(ns backend.static
  (:require [hiccup.core :refer [html]]
            [hiccup.page :refer [html5 include-css include-js]]
            [metosin.ring.util.cache :as cache]
            [metosin.ring.util.hash :as hash]
            [ring.middleware.resource :refer [resource-request]]
            [ring.util.codec :as codec]
            [ring.util.http-response :as resp :refer [ok]]
            [ring.util.request :as request]))

(defn index-page [env devcards?]
  (html
    (html5
      [:head
       [:title "App"]
       [:meta {:charset "utf-8"}]
       [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
       [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
       [:link {:href "https://fonts.googleapis.com/css?family=Roboto:400,300,500,700&subset=latin,latin-ext" :rel "stylesheet" :type "text/css"}]
       (include-css (str "css/main.css?v=" (hash/memo-resource-hash "css/main.css")))]
      [:body
       [:div#app]
       (if (get-in env [:ui :dev-tools])
         [:div#dev])
       (include-js (str "js/main.js?v=" (hash/memo-resource-hash "js/main.js")))])))

(defn index-handler [env]
  (let [index (index-page env false)]
    (fn [request]
      (if (#{:head :get} (:request-method request))
        (let [path (subs (codec/url-decode (request/path-info request)) 1)]
          (some-> (case path
                    "" (ok index)
                    "index.html" (ok index)
                    nil)
                  (cache/cache-control cache/cache-30d)))))))

(defn static-handler [env]
  (let [index (index-handler env)]
    (-> (fn [req]
          (or (index req)
              ; FIXME: Don't serve all files
              ; FIXME: Serve font-awesome
              (resource-request req "")))
        (cache/wrap-cache {:value cache/cache-30d}))))
