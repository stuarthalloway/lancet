(ns lancet.test
  (:use clojure.contrib.test-is))

(def tests [:lancet :runonce :ant :coerce])

(defn test-name
  [test]
  (symbol (str "lancet.test." (name test))))

(doseq [test tests]
  (require (test-name test)))

(doseq [test tests]
  (println "\n\n=====>" test)
  (run-tests (test-name test)))

(shutdown-agents)
