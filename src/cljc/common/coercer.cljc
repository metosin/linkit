(ns common.coercer
  (:require [schema.core :as s]
            [schema.coerce :as sc]))

#?(:cljs (enable-console-print!))

(defn coercion-matcher
  "Use for example for user input -> EDN"
  [schema]
  (sc/string-coercion-matcher schema))

(defn keyword->str-matcher
  "Coerces keywords back to strings. Useful when all map keys are read as keywords
  from MongoDB but some Schema uses Strings for map keys."
  [schema]
  (if (= s/Str schema)
    (fn [x]
      (if (keyword? x)
        (name x)
        x))))

(defn json-matcher
  "Use for example for MongoDB -> EDN"
  [schema]
  (or (keyword->str-matcher schema)
      (sc/json-coercion-matcher schema)))
