(ns lancet.test.coerce
  (:use [clojure.test])
  (:use [lancet.core]))

(deftest boolean-coerce
  (are [_1 _2] (= _1 _2)
       (coerce Boolean/TYPE "yes") true
       (coerce Boolean/TYPE "YES") true
       (coerce Boolean/TYPE "on") true
       (coerce Boolean/TYPE "ON") true
       (coerce Boolean/TYPE "true") true
       (coerce Boolean/TYPE "TRUE") true
       (coerce Boolean/TYPE "no") false
       (coerce Boolean/TYPE "foo") false))

(deftest file-coerce
  (is (= (coerce java.io.File "foo") (java.io.File. "foo"))))

(deftest default-coerce
  (is (= (coerce Comparable 10) 10)))
