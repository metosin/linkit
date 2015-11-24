(ns backend.static
  (:require [hiccup.core :refer [html]]
            [hiccup.page :refer [html5 include-css include-js]]
            [metosin.ring.util.cache :as cache]
            [metosin.ring.util.hash :as hash]
            [ring.middleware.resource :refer [resource-request]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.util.codec :as codec]
            [ring.util.http-response :as resp :refer [ok found]]
            [ring.util.request :as request]))

(defn index-page [dev-tools?]
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
       (if dev-tools?
         [:div#dev])
       (include-js (str "js/main.js?v=" (hash/memo-resource-hash "js/main.js")))])))

(defn index-handler [opts]
  (let [index (index-page opts)]
    (fn [request]
      (if (#{:head :get} (:request-method request))
        (let [path (subs (codec/url-decode (request/path-info request)) 1)]
          (case path
            "" (-> (ok index)
                   (resp/content-type "text/html"))
            "index.html" (found "/")
            nil))))))

(defn static-handler [opts]
  (let [index (index-handler opts)]
    (-> (fn [req]
          (or (index req)
              ; FIXME: Don't serve all files
              ; FIXME: Serve font-awesome
              (resource-request req "")))
        (wrap-content-type)
        (cache/wrap-cache {:value cache/cache-30d}))))
