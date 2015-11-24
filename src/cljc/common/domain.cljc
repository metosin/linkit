(ns common.domain
  (:require [schema.core :as s]
            [schema-tools.core :as st]
            [clojure.string :as string]
            #?(:cljs goog.date.UtcDateTime))
  #?(:clj (:import [org.joda.time.DateTime])))

(defn non-empty-str? [s]
  (seq (string/trim s)))

(s/defschema Link
  {:_id s/Str
   :url (s/constrained s/Str non-empty-str?)
   :title s/Str
   :favicon s/Str
   :dateTime #?(:clj org.joda.time.DateTime :cljs goog.date.UtcDateTime)
   :likes s/Int
   :likeUsers #{s/Str}})

(s/defschema NewLink
  (st/select-keys Link [:url]))
