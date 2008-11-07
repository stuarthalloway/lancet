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

(defn instantiate-data-type [project name props]
  (let [obj (.createDataType project name)]
    (throw-if (nil? obj) (str "No task named " name))
    (set-properties! obj props)
    obj))
    
(defmacro define-ant-task [task-name]
  `(defn ~(symbol (eval task-name))
     ([] (~(symbol (eval task-name)) {}))
     ([props#]
	(let [task# (instantiate-task ant-project ~task-name props#)]
	  (.execute task#)))))

(defmacro define-data-type [type-name]
  `(defn ~(symbol (eval type-name))
     ([] (~(symbol (eval type-name)) {}))
     ([props#]
	(instantiate-data-type ant-project ~type-name props#))))
  
; this would reflect across all task-defs, but it doesn't work. 
; Don't know why yet
;(doseq td (.getTaskDefinitions ant-project)
;  (do
;    (println (.getKey td)))
;    (define-ant-task (.getKey td)))

; TODO: replace with reflective approach (above)
(define-ant-task "echo")
(define-ant-task "tstamp")
(define-ant-task "mkdir")
(define-ant-task "javac")
(define-ant-task "delete")
(define-ant-task "jar")

(define-data-type "fileset")

