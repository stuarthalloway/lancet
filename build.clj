(use 'clojure.contrib.shell-out 'lancet)

(def clojure-home (or (env :CLOJURE_HOME) "/Users/stuart/repos/clojure"))
(def contrib-home (or (env :CLOJURE_CONTRIB_HOME) "/Users/stuart/repos/clojure-contrib"))

(def lib-dir "lib")
(def build-dir "build")

(deftarget build-clojure "Build Clojure from source"
  (with-sh-dir clojure-home
    (system "git svn rebase")
    (system "ant jar")))

(deftarget build-contrib "Build Contrib from source"
  (with-sh-dir contrib-home
    (system "git svn rebase")
    (system "ant clean jar")))

(deftarget init "Prepare for build"
  (mkdir {:dir lib-dir})
  (mkdir {:dir build-dir}))

(deftarget build-dependencies "Build dependent libraries"
  (init)
  (build-clojure) 
  (build-contrib)
  (copy {:file (str clojure-home "/clojure.jar") 
         :todir lib-dir})
  (copy {:file (str contrib-home "/clojure-contrib.jar") 
         :todir lib-dir}))

(deftarget compile-lancet "compile lancet"
  (init)
  (system (str "java -Dclojure.compile.path="
	       build-dir
	       " -cp lib/ant.jar:lib/ant-launcher.jar:lib/clojure.jar:lib/clojure-contrib.jar:build:. clojure.lang.Compile lancet")))

(deftarget create-jar "jar up lancet"
  (init)
  (unjar {:src (str lib-dir "/clojure.jar")
	  :dest build-dir})
  (unjar {:src (str lib-dir "/clojure-contrib.jar")
	  :dest build-dir})
  (unjar {:src (str lib-dir "/ant.jar")
	  :dest build-dir})
  (unjar {:src (str lib-dir "/ant-launcher.jar")
	  :dest build-dir})
  (compile-lancet)
  (jar {:jarfile (str lib-dir "/lancet.jar")
	:basedir "build"
	:manifest "MANIFEST.MF"}))