(defproject pente-cljs "0.1.0-SNAPSHOT"
  :description "Play Pente in a web browser."
  :url "https://github.com/WuHoUnited/pente-cljs"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2322"]
                 [om "0.7.3"]
                 [sablono "0.2.22"]
                 [http-kit "2.1.16"]
                 [prismatic/schema "0.3.0"]]

  :plugins [[lein-cljsbuild "1.0.4-SNAPSHOT"]]

  :source-paths ["src"]

  :profiles {:dev {:plugins [[com.keminglabs/cljx "0.4.0"]]}}
  :cljx {:builds [{:source-paths ["src-cljx"]
                   :output-path "target/classes"
                   :rules :clj}

                  {:source-paths ["src-cljx"]
                   :output-path "target/classes"
                   :rules :cljs}]}
  :hooks [cljx.hooks]

  :cljsbuild {:builds [{:id "prod"
                        :source-paths ["src-cljs" "target/classes"]
                        :compiler {
                                   :output-to "public/prod/pente_cljs.js"
                                   :optimizations :advanced
                                   :pretty-print false
                                   :preamble ["react/react.min.js"]
                                   :externs ["react/externs/react.js"]}}
                       {:id "dev"
                        :source-paths ["src-cljs" "target/classes"]
                        :compiler {
                                   :output-to "public/dev/pente_cljs.js"
                                   :output-dir "out"
                                   :optimizations :none
                                   :source-map true}}]})
