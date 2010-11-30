(ns lancet.test.coerce
  (:use clojure.contrib.test-is)
  (:use lancet))

(deftest boolean-coerce
  (are (= _1 _2)
    (coerce Boolean/TYPE "yes") true
    (coerce Boolean/TYPE "YES") true
    (coerce Boolean/TYPE "on") true
    (coerce Boolean/TYPE "ON") true
    (coerce Boolean/TYPE "true") true
    (coerce Boolean/TYPE "TRUE") true
    (coerce Boolean/TYPE "no") false
    (coerce Boolean/TYPE "foo") false))

(deftest file-coerce
  (are (= _1 _2)
    (coerce java.io.File "foo") (java.io.File. "foo")))

(deftest default-coerce
  (are (= _1 _2)
    (coerce Comparable 10) 10
    (coerce Integer/TYPE 10) 10))