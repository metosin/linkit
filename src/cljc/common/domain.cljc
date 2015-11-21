(ns common.domain
  (:require [schema.core :as s]
            [schema-tools.core :as st]
            [metosin.dates :as dates]
            [clojure.string :as string]))

(defn non-empty-str? [s]
  (seq (string/trim s)))

(s/defschema Link
  {:_id s/Str
   :url (s/constrained s/Str non-empty-str?)
   :title s/Str
   :favicon s/Str
   :dateTime dates/DateTime
   :likes s/Int
   :likeUsers #{s/Str}})

(s/defschema NewLink
  (st/select-keys Link [:url]))
