;;  Copyright (c) Relevance, Inc. All rights reserved.
;;  The use and distribution terms for this software are covered by the
;;  Common Public License 1.0 (http://opensource.org/licenses/cpl.php)
;;  which can be found in the file CPL.TXT at the root of this distribution.
;;  By using this software in any fashion, you are agreeing to be bound by
;;  the terms of this license.
;;  You must not remove this notice, or any other, from this software.

(ns lancet.ant
    (use clojure.contrib.except lancet))

(def
 #^{:doc "Dummy ant project to keep Ant tasks happy"}
 ant-project 
 (let [proj (org.apache.tools.ant.Project.)
       logger (org.apache.tools.ant.NoBannerLogger.)]
   (doto logger
     (setMessageOutputLevel org.apache.tools.ant.Project/MSG_INFO)
     (setOutputPrintStream System/out)
     (setErrorPrintStream System/err))
   (doto proj
     (init)
     (addBuildListener logger))
   proj))

(defmethod lancet/coerce [org.apache.tools.ant.types.Path String] [_ str] 
  (org.apache.tools.ant.types.Path. ant-project str))

(defn instantiate-data-type [project name props]
  (let [obj (.createDataType project name)]
    (throw-if (nil? obj) (str "No task named " name))
    (set-properties! obj props)
    obj))


;; commented out target stuff becomes necessary if some tasks depend on a (dummy?) target
(defn instantiate-task [project name props]
  (let [task (.createTask project name)]
    (throw-if (nil? task) (str "No task named " name))
    (doto task
      (init)
      (setProject project))
      ; (setOwningTarget target)
    (set-properties! task props)
    ; (.addTask target task)
    task))

(defn create-ant-task [task-name]
  (fn f
    ([] 
       (f {}))
    ([props]
       (let [task (instantiate-task ant-project task-name props)]
	 (.execute task)))))

(def task-names 
     (disj (set (map #(symbol (.getKey %)) (.getTaskDefinitions ant-project)))
	   'import 'touch 'sync 'concat 'filter 'replace 'get 'apply))

(defmacro define-ant-task [t]
  `(def ~t (create-ant-task ~(name t))))

(defmacro define-ant-tasks []
  `(do
     ~@(map 
	(fn [nm]
	  `(define-ant-task ~nm))
	task-names)))

(define-ant-tasks)

; (define-data-type fileset)

