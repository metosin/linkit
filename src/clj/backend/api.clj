(ns backend.api
  (:require [common.domain :as domain]
            [kekkonen.cqrs :refer :all]
            [plumbing.core :refer [defnk]]
            [schema.core :as s]
            [metosin.dates :as dates]
            [monger.operators :refer :all]
            [monger.collection :as mc]
            [monger.joda-time]
            [clojure.java.io :as io]
            [clj-http.client :as http]
            [ring.util.http-predicates :refer [ok?]]
            [potpuri.core :as p]
            [net.cgrand.enlive-html :as html]))

(defnk ^:query all [db]
  (success (mc/find-maps db :links nil {:likeUsers 0})))

(defnk ^:query by-id [db [:data link-id]]
  (success (mc/find-map-by-id db :links link-id {:likeUsers 0})))

(defn get-info [url]
  (let [res (http/get url {:as :stream})]
    (if (ok? res)
      (let [url' (java.net.URL. url)
            baseurl (str (.getProtocol url') "://" (.getHost url'))
            html (html/html-resource (:body res))
            icons (into {} (map (juxt :sizes :href) (map :attrs (html/select html [:head [:link (html/attr-has :rel "icon")]]))))
            icon (or (get icons "96x96")
                     (get icons "64x64")
                     (get icons "32x32")
                     (get icons "16x16"))
            icon-url (cond
                       (.startsWith icon "http") icon
                       (.startsWith icon "/") (str baseurl icon)
                       :else (str baseurl "/" icon))]
        {:title (html/text (first (html/select html [:head :title])))
         :favicon icon-url}))))

(comment
  (get-info "http://clojars.org"))

(defnk ^:command add [db, data :- domain/NewLink]
  (mc/insert db :links (merge data
                              (get-info (:url data))
                              {:dateTime (dates/now)
                               :_id (str (org.bson.types.ObjectId.))
                               :likes 0
                               :likeUsers #{}}))
  (success {:status :ok}))

(defnk ^:command like
  "Adds like to the link if user hasn't liked this link already."
  [db, [:data link-id :- s/Str user :- s/Str]]
  (mc/update db :links
             {:_id link-id
              :likeUsers {$ne user}}
             {$addToSet {:likeUsers user}
              $inc {:likes 1}})
  (success {:status :ok}))

(defnk ^:command dislike
  "Removes users like from the link."
  [db, [:data link-id :- s/Str user :- s/Str]]
  (mc/update db :links
             {:_id link-id
              :likeUsers user}
             {$pull {:likeUsers user}
              $inc {:likes -1}})
  (success {:status :ok}))

(comment
  (get-links (user/ctx))
  (add-link (merge (user/ctx) {:data {:url "http://clojars.org"}}))
  (like-link (merge (user/ctx) {:data {:link-id "5650b5e6f7ff9b5ff023077b" :user "Foo"}}))
  (dislike-link (merge (user/ctx) {:data {:link-id "5650b5e6f7ff9b5ff023077b" :user "Foo"}})))
