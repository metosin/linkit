(ns backend.impl
  (:require [clj-http.client :as http]
            [net.cgrand.enlive-html :as html]
            [ring.util.http-predicates :refer [ok?]])
  (:import java.net.URL))

(defn get-info
  "Scrapes the given url's title and favicon."
  [url]
  (let [res (http/get url {:as :stream})]
    (if (ok? res)
      (let [url' (URL. url)
            baseurl (str (.getProtocol url') "://" (.getHost url'))
            html (html/html-resource (:body res))
            icons (into {} (map (juxt :sizes :href) (map :attrs (html/select html [:head [:link (html/attr-has :rel "icon")]]))))
            icon (or (get icons "96x96")
                     (get icons "64x64")
                     (get icons "32x32")
                     (get icons "16x16")
                     (get icons nil))
            icon-url (if icon
                       (cond
                         (.startsWith icon "http") icon
                         (.startsWith icon "/") (str baseurl icon)
                         :else (str baseurl "/" icon)))]
        {:title (html/text (first (html/select html [:head :title])))
         :favicon icon-url}))))

(comment
  (get-info "http://clojars.org"))

