(ns metosin.transit
  (:require [cognitect.transit :as transit]
            [clojure.string :as string]
            #?@(:cljs [[goog.string :as gs]
                       goog.date.UtcDateTime
                       goog.date.Date]))
  #?(:clj  (:import [org.joda.time])))

(def DateTime #?(:clj org.joda.time.DateTime, :cljs goog.date.UtcDateTime))
(def LocalDate #?(:clj org.joda.time.LocalDate, :cljs goog.date.Date))

(defn unparse-iso [d]
  ; This is RFC3339?
  #?(:clj  (.toString (.withZone d (org.joda.time.DateTimeZone/forID "UTC")))
     :cljs (str (.getUTCFullYear d)
                "-" (gs/padNumber (inc (.getUTCMonth d)) 2)
                "-" (gs/padNumber (.getUTCDate d) 2)
                "T" (gs/padNumber (.getUTCHours d) 2)
                ":" (gs/padNumber (.getUTCMinutes d) 2)
                ":" (gs/padNumber (.getUTCSeconds d) 2)
                "." (gs/padNumber (.getUTCMilliseconds d) 3)
                "Z")))

(defn parse-iso [s]
  #?(:clj  (org.joda.time.DateTime/parse s)
     :cljs (goog.date.UtcDateTime.fromIsoString s)))

(defn unparse-local-date [x]
  #?(:clj  (.toString x)
     :cljs (.toIsoString x true false)))

(defn parse-local-date [x]
  #?(:clj  (org.joda.time.LocalDate/parse x)
     :cljs (let [[_ y m d] (re-find #"(\d{4})-(\d{2})-(\d{2})" x)]
             (goog.date.Date.
               (long y)
               (dec (long m))
               (long d)))))

(def writers
  {DateTime
   (transit/write-handler (constantly "DateTime") unparse-iso)

   LocalDate
   (transit/write-handler (constantly "Date") unparse-local-date)})

(def readers
  {"DateTime" (transit/read-handler parse-iso)
   "Date"     (transit/read-handler parse-local-date)})
