(ns metosin.dates
  "Use this namespace to format dates and datetimes for user.

   Don't use for serializing or deserializing.

   Clojure side uses always Helsinki timezone.
   On Cljs side, uses the timezone of browser."
  #?(:cljs (:require goog.date.UtcDateTime
                     goog.date.Date
                     goog.i18n.DateTimeFormat))
  #?(:clj  (:import [org.joda.time DateTimeZone])))

; FIXME: No hardcoding
#?(:clj (def helsinki-tz (DateTimeZone/forID "Europe/Helsinki")))
; (def date-fmt       "d.M.yyyy")
; (def date-time-fmt  "d.M.yyyy H:mm")

;;
;; Types
;;

(def DateTime #?(:clj  org.joda.time.DateTime,
                 :cljs goog.date.UtcDateTime))
(def LocalDate #?(:clj  org.joda.time.LocalDate,
                  :cljs goog.date.Date))


;;
;; Conversions
;;

(defprotocol ToNative
  (to-native [x] "Convers to native Date object (java.util.Date or js/Date)."))

#?(:clj
    (extend-protocol ToNative
      org.joda.time.DateTime
      (to-native [x] (.toDate x))
      org.joda.time.LocalDate
      (to-native [x] (.toDate x))))

(defprotocol ToDateTime
  (-to-date-time [x] "Convers Date or such to DateTime."))

#?(:cljs
    (extend-protocol ToDateTime
      goog.date.Date
      (-to-date-time [x]
        (goog.date.UtcDateTime. (.getYear x) (.getMonth x) (.getDate x)))))

(defprotocol ToDate
  (-to-date [x] "Convers DateTime or such to Date."))

#?(:clj
   (extend-protocol ToDate
     java.util.Date
     (-to-date [x]
       (org.joda.time.LocalDate/fromDateFields x))
     nil
     (-to-date [x]
       nil))
   :cljs
   (extend-protocol ToDate
      goog.date.UtcDateTime
      (-to-date [x]
        (goog.date.Date. (.getYear x) (.getMonth x) (.getDate x)))))

; FIXME: Is this a good idea?
; Required for using dates as keys etc.
#?(:cljs
    (extend-type goog.date.Date
      IEquiv
      (-equiv [o other]
        (and (instance? goog.date.Date other)
             (identical? (.getTime o) (.getTime other))
             (identical? (.getTimezoneOffset o) (.getTimezoneOffset other))))
      IComparable
      (-compare [o other]
        (- (.getTime o) (.getTime other)))))

;;
;; Constructors
;;

(defn date-time
  ([x] (-to-date-time x))
  ([y m d hh mm] #?(:clj  (org.joda.time.DateTime. y m d hh mm)
                    :cljs (goog.date.UtcDateTime.  y m d hh mm)))
  ([y m d hh mm ss] #?(:clj  (org.joda.time.DateTime. y m d hh mm ss)
                       :cljs (goog.date.UtcDateTime.  y m d hh mm ss))))

(defn date
  ([x] (-to-date x))
  ([y m d]
   #?(:clj  (org.joda.time.LocalDate. y m d)
      :cljs (goog.date.Date. y m d))))

(defn now []
  #?(:clj  (org.joda.time.DateTime.)
     :cljs (goog.date.UtcDateTime.)))

(defn today []
  #?(:clj  (org.joda.time.LocalDate.)
     :cljs (goog.date.Date.)))

;;
;; Parsing and unparsing
;;

(defn formatter [f]
  ; TODO:
  #?(:cljs (goog.i18n.DateTimeFormat. f)))

(defn unparse [f x]
  ; TODO:
  (.format (if (string? f) (formatter f) f) x))

(defn parse [f x]
  ; TODO:
  nil)

(defn start-of-week [date]
  (let [week-day (.getIsoWeekday date)
        day (.getDate date)]
  ; TODO:
    #?(:cljs (goog.date.Date. (.getYear date) (.getMonth date) (- day week-day)))))

(defn end-of-week [date]
  (let [week-day (.getIsoWeekday date)
        day (.getDate date)]
  ; TODO:
    #?(:cljs (goog.date.Date. (.getYear date) (.getMonth date) (+ day (- 6 week-day))))))

(defn add [date x]
  ; TODO:
  {:pre [#?(:cljs (instance? goog.date.Interval x))]}
  (let [n (.clone date)]
    (.add n x)
    n))

(defn days [n]
  ; TODO:
  #?(:cljs (goog.date.Interval. goog.date.Interval.DAYS n)))

(comment
  (date-time->str (date-time 2015 10 6 7 40)))
