(ns metosin.dates-test
  (:require #?(:clj  [clojure.test :refer :all]
               :cljs [cljs.test :refer-macros [deftest is]])
            [metosin.dates :as d]))

(deftest date-test
  (is (= "6.10.2015" (d/unparse "d.M.yyyy" (d/date 2015 10 6))))
  (is (= "6.5.2015" (d/unparse "d.M.yyyy" (d/date 2015 5 6)))))

(deftest date-time-test
  (is (= "6.10.2015 10:40" (d/unparse "d.M.yyyy H:MM" (d/date-time 2015 10 6 7 40)))
      "helsinki tz")
  (is (= "7.1.2015 1:30" (d/unparse "d.M.yyyy H:MM" (d/date-time 2015 1 6 23 30)))
      "normal time")
  (is (= "7.5.2015 2:30" (d/unparse "d.M.yyyy H:MM" (d/date-time 2015 5 6 23 30)))
      "summer time"))
