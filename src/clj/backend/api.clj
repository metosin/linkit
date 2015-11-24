(ns backend.api
  (:require [backend.impl :as impl]
            [common.domain :as domain]
            [kekkonen.cqrs :refer :all]
            [monger.collection :as mc]
            monger.joda-time
            [monger.operators :refer :all]
            [plumbing.core :refer [defnk fnk]]
            [schema.core :as s])
  (:import [org.joda.time DateTime]))

;;
;; Context handlers
;;

(defn load-link
  "Ensures that there is a link-id parameter. Associates the referred link
  to context or returns an error if it doesn't exist."
  [_]
  (fnk [db [:data link-id :- s/Str] :as ctx]
    (if-let [link  (mc/find-map-by-id db :links link-id)]
      (assoc-in ctx [:entity :link] link)
      (failure! {:status :link-doesnt-exist
                 :link-id link-id}))))

(defn require-liked
  "Ensures that the user has or has not liked the referred link."
  [liked?]
  (fnk [[:entity [:link likeUsers]]
        [:data user]
        :as context]
    (if (= liked? (contains? (set likeUsers) user))
      context
      (failure! {:status (if liked? :cant-dislike-not-liked :cant-like-already-liked)
                 :liked? liked?}))))

;;
;; Queries
;;

(defnk ^:query all
  "Returns all the links"
  [db]
  (success (into [] (mc/find-maps db :links nil))))

(defnk ^:query by-id
  "Returns a single link by given id"
  {::load-link true}
  [[:entity link]]
  (success link))

;;
;; Commands
;;

(defnk ^:command add
  "Adds a single link to database"
  [db, data :- domain/NewLink]
  (try
    (mc/insert db :links (merge data
                                (impl/get-info (:url data))
                                {:dateTime (DateTime.)
                                 :_id (str (org.bson.types.ObjectId.))
                                 :likes 0
                                 :likeUsers #{}}))
    (success {:status :ok})
    (catch Exception e
      (failure {:status :error}))))

(defnk ^:command like
  "Adds like to the link if user hasn't liked this link already."
  {::load-link true
   ::liked? false}
  [db, [:data link-id :- s/Str user :- s/Str]]
  (mc/update db :links
             {:_id link-id
              :likeUsers {$ne user}}
             {$addToSet {:likeUsers user}
              $inc {:likes 1}})
  (success {:status :ok}))

(defnk ^:command dislike
  "Removes users like from the link."
  {::load-link true
   ::liked? true}
  [db, [:data link-id :- s/Str user :- s/Str]]
  (mc/update db :links
             {:_id link-id
              :likeUsers user}
             {$pull {:likeUsers user}
              $inc {:likes -1}})
  (success {:status :ok}))
