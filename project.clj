(defproject pente-cljs "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2322"]
                 [om "0.7.3"]
                 [sablono "0.2.22"]
                 [http-kit "2.1.16"]]

  :plugins [[lein-cljsbuild "1.0.4-SNAPSHOT"]]

  :source-paths ["src"]

  :cljsbuild {:builds [{:id "prod"
                        :source-paths ["src-cljs"]
                        :compiler {
                                   :output-to "public/prod/pente_cljs.js"
                                   :optimizations :advanced
                                   :pretty-print false
                                   :preamble ["react/react.min.js"]
                                   :externs ["react/externs/react.js"]}}
                       {:id "dev"
                        :source-paths ["src-cljs"]
                        :compiler {
                                   :output-to "public/dev/pente_cljs.js"
                                   :output-dir "out-dev"
                                   :optimizations :none
                                   :source-map true}}]})
