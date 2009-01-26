(use 'clojure.contrib.shell-out 'lancet)

(def clojure-home (or (env :CLOJURE_HOME) "/Users/stuart/repos/clojure"))
(def contrib-home (or (env :CLOJURE_CONTRIB_HOME) "/Users/stuart/repos/clojure-contrib"))
(def lib-dir "lib")

(deftarget build-clojure "Build Clojure from source"
  (with-sh-dir clojure-home
    (system "git svn rebase")
    (system "ant jar")))

(deftarget build-contrib "Build Contrib from source"
  (with-sh-dir contrib-home
    (system "git svn rebase")
    (system "ant clean jar")))

(deftarget init "Prepare for build"
  (mkdir {:dir lib-dir}))

(deftarget calls-init "Test calling init"
  (init)
  (println "called init"))

(deftarget build-dependencies "Build dependent libraries"
  (init)
  (build-clojure) 
  (build-contrib)
  (copy {:file (str clojure-home "/clojure.jar") 
         :todir lib-dir})
  (copy {:file (str contrib-home "/clojure-contrib.jar") 
         :todir lib-dir}))