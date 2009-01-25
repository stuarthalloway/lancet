(ns lancet.test.lancet
  (:use clojure.contrib.test-is [clojure.set :only (intersection)])
  (:use lancet))

;; Predicates

(deftest test-has-run?
  (def #^{:has-run (fn [] :bang)} fn#)
  (is (= :bang (has-run? fn#))))

(deftest test-reset
  (def #^{:reset-fn (fn [] :zap)} fn#)
  (is (= :zap (reset fn#))))

(deftest test-task-names
  (let [some-names #{'echo 'mkdir}]
    (are (= _1 _2)
	 (intersection (into #{} (task-names)) some-names) some-names)))

(deftest test-safe-ant-name
  (are (= _1 _2)
       (safe-ant-name 'echo) 'echo
       (safe-ant-name 'import) 'ant-import))

(deftest test-define-all-ant-tasks-defines-echo
  (let [echo-task (echo {:description "foo"})]
    (are (= _1 _2)
	 (.getDescription echo-task) "foo"
	 (class echo-task) org.apache.tools.ant.taskdefs.Echo)))
  