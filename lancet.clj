(ns lancet
  (:use [clojure.contrib.except :only (throw-if)])
  (:import (java.beans Introspector)))

(def
 #^{:doc "Dummy ant project to keep Ant tasks happy"} 	
 ant-project                                            
 (let [proj (org.apache.tools.ant.Project.)             
       logger (org.apache.tools.ant.NoBannerLogger.)]
   (doto logger                                         
     (.setMessageOutputLevel org.apache.tools.ant.Project/MSG_INFO)
     (.setOutputPrintStream System/out)
     (.setErrorPrintStream System/err))
   (doto proj                                           
     (.init)                                                                             
     (.addBuildListener logger))))

(defn property-descriptor [inst prop-name]
  (first
   (filter #(= prop-name (.getName %)) 
	   (.getPropertyDescriptors 
	    (Introspector/getBeanInfo (class inst))))))

(defn set-property! [inst prop value]
  (let [pd (property-descriptor inst prop)]   
    (throw-if (nil? pd) (str "No such property " prop)) 
    (.invoke (. pd getWriteMethod) inst (into-array [value])))) 

(defn set-properties! [inst prop-map]
  (doseq [[k v] prop-map] (set-property! inst (name k) v))) 

(defn instantiate-task [project name props]
  (let [task (.createTask project name)]
    (throw-if (nil? task) (str "No task named " name))
    (doto task
      (.init)
      (.setProject project)) 
    (set-properties! task props) 
    task))

(defn runonce
 "Create a function that will only run once. All other invocations
 return the first calculated value. The function can have side effects.
 Returns a [has-run-predicate, reset-fn, once-fn]"
 [function]
 (let [sentinel (Object.) 
       agt (agent sentinel)
       reset-fn (fn [] (send agt (fn [_] sentinel)) (await agt))
       has-run? #(not= @agt sentinel)]
   [has-run?
    reset-fn
    (fn [& args] 
      (when (= @agt sentinel)
	(send-off agt
		  #(if (= % sentinel)
		     (apply function args)
		     %))
	(await agt))
      @agt)]))

(defmacro has-run? [f]
  `((:has-run (meta (var ~f)))))

(defmacro reset [f]
  `((:reset-fn (meta (var ~f)))))

(defmacro deftarget [sym doc & forms]
  (let [has-run (gensym "hr-") reset-fn (gensym "rf-")]
    `(let [[~has-run ~reset-fn once-fn#] (runonce (fn [] ~@forms))]
       (def ~(with-meta sym {:doc doc :has-run has-run :reset-fn reset-fn}) 
	    once-fn#))))

(defmacro define-ant-task [clj-name ant-name]
  `(defn ~clj-name [props#]
     (let [task# (instantiate-task ant-project ~(name ant-name) props#)]
       (.execute task#)
       task#)))

(defn task-names [] (map symbol (seq (.. ant-project getTaskDefinitions keySet))))

(defn safe-ant-name [n]
  (if (ns-resolve 'clojure.core n) (symbol (str "ant-" n)) n))

(defmacro define-all-ant-tasks []
  `(do ~@(map (fn [n] `(define-ant-task ~n ~n)) (task-names))))

(defmacro define-all-ant-tasks []
  `(do ~@(map (fn [n] `(define-ant-task ~(safe-ant-name n) ~n)) (task-names))))

(define-all-ant-tasks)



