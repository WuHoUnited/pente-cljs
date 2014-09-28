(ns leiningen.make
  "Leiningen task to make a project."
  (:require [leiningen.cljx :as cljx]
            [leiningen.cljsbuild :as cljsbuild]))

(defmacro with-audit!
  "This will print a logging message to *out*
  before and after the body is executed"
  [name & body]
  `(do
     (println "------------------")
     (println "Beginning" ~name)
     ~@body
     (println "Finished" ~name)))

(defn help!
  "Print out help text"
  []
  (let [commands ["once" "auto"]]
    (println (str "The only valid arguments are (optionally followed by a build name):\n"
                  (apply str
                         (interpose \newline commands))))))

(defn run-cljx!
  "Run cljx with the given arguments"
  [project arg]
  (with-audit! "cljx"
    (cljx/cljx project arg)))

(defn run-cljsbuild!
  "Run cljsbuild with the given arguments"
  [project & args]
  (with-audit! "cljsbuild"
    (apply cljsbuild/cljsbuild project args)))

(defn once!
  "Handle the once command"
  [project args]
  (run-cljx! project "once")
  (apply run-cljsbuild! project "once" args))

(defn auto!
  "Handle the auto command"
  [project args]
  ; We have to run cljx first so that cljsbuild has everything it needs
  (run-cljx! project "once")
  (future (run-cljx! project "auto"))
  (apply run-cljsbuild! project "auto" args))

(defn make
  "Build the project, currently the only valid arguments are once and auto"
  [project & args]
  (case (first args)
    "once" (once! project (rest args))
    "auto" (auto! project (rest args))
    (help!)))
