(ns backend.api
  (:require [common.domain :as domain]
            [kekkonen.cqrs :refer :all]
            [plumbing.core :refer [defnk]]
            [schema.core :as s]))

(defnk ^:query get-links [db]
  (success []))

(defnk ^:command add-link [db, conn, data :- domain/Link]
  (success nil))

(defnk ^:command like-link [db, conn, [:data link-id :- s/Str user-id :- s/Str]]
  (success nil))

(defnk ^:command dislike-link [db, conn, [:data link-id :- s/Int user-id :- s/Int]]
  (success nil))
