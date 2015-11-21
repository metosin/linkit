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
   :dateTime dates/DateTime
   :likes #{s/Str}})

(s/defschema NewLink
  (st/dissoc Link :_id :dateTime :likes))
