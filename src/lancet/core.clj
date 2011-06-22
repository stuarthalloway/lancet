(ns lancet.core
  (:gen-class)
  (:import (java.beans Introspector)
           (java.util.concurrent CountDownLatch)
           (org.apache.tools.ant.types Path)
           (org.apache.tools.ant.taskdefs Manifest$Attribute)
           (java.util Map)))

(def #^{:doc "Dummy ant project to keep Ant tasks happy"}
  ant-project
  (let [proj (org.apache.tools.ant.Project.)
        logger (org.apache.tools.ant.NoBannerLogger.)]
    (doto logger
      (.setMessageOutputLevel org.apache.tools.ant.Project/MSG_INFO)
      (.setEmacsMode true)
      (.setOutputPrintStream System/out)
      (.setErrorPrintStream System/err))
    (doto proj
      (.init)
      (.addBuildListener logger))))

(defmulti coerce (fn [dest-class src-inst] [dest-class (class src-inst)]))

(defmethod coerce [java.io.File String] [_ str]
  (java.io.File. str))
(defmethod coerce [Boolean/TYPE String] [_ str]
  (contains? #{"on" "yes" "true"} (.toLowerCase str)))
(defmethod coerce :default [dest-cls obj] (cast dest-cls obj))
(defmethod coerce [Path String] [_ str]
  (Path. ant-project str))

(defn env [val]
  (System/getenv (name val)))

(defn- build-sh-args [args]
  (concat (.split (first args) " +") (rest args)))

(defn property-descriptor [inst prop-name]
  (first
   (filter #(= prop-name (.getName %))
           (.getPropertyDescriptors
            (Introspector/getBeanInfo (class inst))))))

(defn get-property-class [write-method]
  (first (.getParameterTypes write-method)))

(defn set-property! [inst prop value]
  (let [pd (property-descriptor inst prop)]
    (when-not pd
      (throw (Exception. (format "No such property %s." prop))))
    (let [write-method (.getWriteMethod pd)
          dest-class (get-property-class write-method)]
      (.invoke write-method inst (into-array [(coerce dest-class value)])))))

(defn set-properties! [inst prop-map]
  (doseq [[k v] prop-map] (set-property! inst (name k) v)))

(def ant-task-hierarchy
  (atom (-> (make-hierarchy)
            (derive ::exec ::has-args))))

(defmulti add-nested
  "Adds a nested element to ant task.
Elements are added in a different way for each type.
Task name keywords are connected into a hierarchy which can
be used to extensively add other types to this method.
The default behaviour is to add an element as a fileset."
  (fn [name task nested] [(keyword "lancet.core" name) (class nested)])
  :hierarchy ant-task-hierarchy)

(defmethod add-nested [::manifest Map]
  [_ task props]
  (doseq [[n v] props] (.addConfiguredAttribute task
                                                (Manifest$Attribute. n v))))
(defmethod add-nested [::has-args String]
  [_ task arg]
  (doto  (.createArg task) (.setValue arg)))

(defmethod add-nested :default [_ task nested] (.addFileset task nested))

(defn instantiate-task [project name props & nested]
  (let [task (.createTask project name)]
    (when-not task
      (throw (Exception. (format "No task named %s." name))))
    (doto task
      (.init)
      (.setProject project)
      (set-properties! props))
    (doseq [n nested]
      (add-nested name task n)
      )
    task))

(defn runonce
  "Create a function that will only run once. All other invocations
  return the first calculated value. The function *can* have side effects
  and calls to runonce *can* be composed. Deadlock is possible
  if you have circular dependencies.
  Returns a [has-run-predicate, reset-fn, once-fn]"
  [function]
  (let [sentinel (Object.)
        result (atom sentinel)
        reset-fn (fn [] (reset! result sentinel))
        has-run-fn (fn [] (not= @result sentinel))]
    [has-run-fn
     reset-fn
     (fn [& args]
       (locking sentinel
         (if (= @result sentinel)
           (reset! result (function))
           @result)))]))

(defmacro has-run? [f]
  `((:has-run (meta (var ~f)))))

(defmacro reset [f]
  `((:reset-fn (meta (var ~f)))))

(def targets (atom #{}))

(defmacro deftarget [sym doc & forms]
  (swap! targets #(conj % sym))
  (let [has-run (gensym "hr-") reset-fn (gensym "rf-")]
    `(let [[~has-run ~reset-fn once-fn#] (runonce (fn [] ~@forms))]
       (def ~(with-meta sym {:doc doc :has-run has-run :reset-fn reset-fn})
            once-fn#))))

(defmacro define-ant-task [clj-name ant-name]
  `(defn ~clj-name [& props#]
     (let [task# (apply instantiate-task ant-project ~(name ant-name) props#)]
       (.execute task#)
       task#)))

(defmacro define-ant-type [clj-name ant-name]
  `(defn ~clj-name [props#]
     (let [bean# (new ~ant-name)]
       (set-properties! bean# props#)
       (when (property-descriptor bean# "project")
         (set-property! bean# "project" ant-project))
       bean#)))

(defn task-names [] (map symbol (seq (.. ant-project getTaskDefinitions keySet))))

(defn safe-ant-name [n]
  (if (ns-resolve 'clojure.core n) (symbol (str "ant-" n)) n))

(defmacro define-all-ant-tasks []
  `(do ~@(map (fn [n] `(define-ant-task ~n ~n)) (task-names))))

(defmacro define-all-ant-tasks []
  `(do ~@(map (fn [n] `(define-ant-task ~(safe-ant-name n) ~n)) (task-names))))

(define-all-ant-tasks)

;; Newer versions of ant don't have this class:
;; (define-ant-type files org.apache.tools.ant.types.resources.Files)
(define-ant-type fileset org.apache.tools.ant.types.FileSet)

(defn -main [& targs]
  (load-file "build.clj")
  (if targs
    (doseq [targ (map symbol targs)]
      (eval (list targ)))
    (println "Available targets: " @targets)))
