(ns lancet.test.runonce
  (:use clojure.contrib.test-is)
  (:use lancet))


(def counter (ref 0))
(defn inc-counter [] (dosync (alter counter inc)))
(defn zero-counter! [] (dosync (ref-set counter 0)))

(deftest test-runonce
  (zero-counter!)
  (let [[has-run? reset f] (runonce inc-counter)]

    ; TODO: add nested contexts to test-is, a la PCL
    ; initial state
    (are (= _1 _2)
     (has-run?) false
     @counter 0)
    
    ; run the fn
    (are (= _1 _2)
     (f) 1
     (has-run?) true
     @counter 1)

    ; run the fn again (no change)
    (are (= _1 _2)
     (f) 1
     (has-run?) true
     @counter 1)

    ; reset the fn
    (reset)
    (are (= _1 _2) 
     (has-run?) false)

    ; run the fn again
    (are (= _1 _2)
     (f) 2
     (has-run?) true
     @counter 2)
))
