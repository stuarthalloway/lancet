;;  Copyright (c) Relevance, Inc. All rights reserved.
;;  The use and distribution terms for this software are covered by the
;;  Common Public License 1.0 (http://opensource.org/licenses/cpl.php)
;;  which can be found in the file CPL.TXT at the root of this distribution.
;;  By using this software in any fashion, you are agreeing to be bound by
;;  the terms of this license.
;;  You must not remove this notice, or any other, from this software.

(ns lancet
    (use clojure.contrib.except))

(def *target*) 
(def *task*)

;; ---------------------------------------------------------------------------------
;; private field
;; ---------------------------------------------------------------------------------
(defn get-private-field 
  "use Java reflection to directly access a private field. Field must
  be declared on the class of inst, not a base class."
  [inst field-name] 
  (let [fld (. (class inst) getDeclaredField field-name)]
    (. fld setAccessible true)
    (. fld get inst)))

;; ---------------------------------------------------------------------------------
;; property descriptor -- primarily for Ant interop
;; ---------------------------------------------------------------------------------

(defn string-to-boolean [str]
  (contains? #{"on" "yes" "true"} (.toLowerCase str)))

(defmulti coerce (fn [prop-desc arg] [(first (.. prop-desc getWriteMethod getParameterTypes)) (class arg)]))
(defmethod coerce [java.io.File String] [_ str] 
  (java.io.File. str))
(defmethod coerce [Boolean/TYPE String] [_ str]
  (string-to-boolean str))
(defmethod coerce :default [_ obj] obj)

;; if the property functions need to be heavily used they could cache reflective data
;; for efficiency
(defn property-descriptor [inst prop]
  (let [info (java.beans.Introspector/getBeanInfo (class inst))]
    (first (filter #(= (name prop) (. % getName)) (.getPropertyDescriptors info)))))

(defn get-property [inst prop]
  (let [pd (property-descriptor inst prop)]
    (throw-if (nil? pd) (str "No such property" prop))
    (.invoke (. pd getReadMethod) inst nil)))

(defn set-property! [inst prop value]
  (let [pd (property-descriptor inst prop)]
    (throw-if (nil? pd) (str "No such property" prop))
    (.invoke (. pd getWriteMethod) inst (into-array [(coerce pd value)]))))

(defn set-properties! [inst prop-map]
  (doseq [k v] prop-map (set-property! inst k v))) 

;; ---------------------------------------------------------------------------------
;; deftarget support
;; ---------------------------------------------------------------------------------
(defn runonce 
  "Create a function that will only run once, given a function. Returns
  a vector containing the function and the reference that tracks whether
  the function has been run."
  [function]
  (let [has-run (ref false)]
    [(fn [& args]
       (or @has-run
	   ; TODO: think through semantics for parallel target invocation
	   (do 
	     (apply function args)
	     (dosync (ref-set has-run true)))))
     has-run]))

(defmacro defrunonce [sym doc & forms]
 "Defines a function with runonce semantics. Curren run status
 is kept in a reference under the :has-run metadata key."
 (let [has-run (gensym)]
   `(let [[function# ~has-run] (runonce (fn [] ~@forms))]
      (def ~(with-meta sym {:has-run has-run}) function#))))

(defmacro deftarget 
  "create a lancet target. Targets are runonce, and get a default docstring
  if you do not specify one."
  [sym & forms]
  (if (string? (first forms))
    `(defrunonce ~sym ~(first forms) [] ~@(rest forms))
    `(defrunonce ~sym "a lancet target" [] ~@forms)))

(defmacro reset 
  "reset a function created with runonce, so it will run again"
  [sym]
  `(dosync (ref-set (:has-run ^#'~sym) false)))
	
(defmacro target-run? [sym]
  "has target been run?"
  [sym]
  `@(:has-run ^#'~sym))

		 

