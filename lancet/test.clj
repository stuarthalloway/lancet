(ns lancet.test
  (:use clojure.contrib.test-is))

(def tests [:lancet :runonce :ant :coerce])

(defn test-name
  [test]
  (symbol (str "lancet.test." (name test))))

(doseq [test tests]
  (require (test-name test)))

(apply run-tests (map test-name tests))

(shutdown-agents)
