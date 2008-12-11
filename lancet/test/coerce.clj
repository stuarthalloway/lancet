(ns lancet.test.coerce
  (:use clojure.contrib.test-is)
  (:use lancet))

(deftest boolean-coerce
  (are =
    (coerce Boolean/TYPE "yes") true
    (coerce Boolean/TYPE "YES") true
    (coerce Boolean/TYPE "on") true
    (coerce Boolean/TYPE "ON") true
    (coerce Boolean/TYPE "true") true
    (coerce Boolean/TYPE "TRUE") true
    (coerce Boolean/TYPE "no") false
    (coerce Boolean/TYPE "foo") false))

(deftest file-coerce
  (are =
    (coerce java.io.File "foo") (java.io.File. "foo")))

(deftest default-coerce
  (are =
    (coerce Comparable 10) 10))