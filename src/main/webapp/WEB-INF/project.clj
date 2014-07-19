(defproject app "0.1.0"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2268"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [cljs-http "0.1.13"]
                 [reagent "0.4.2"]
                 [secretary "1.2.0"]]

  :plugins [[lein-cljsbuild "1.0.3"]]

  :source-paths ["src"]

  :cljsbuild {:builds {:dev
                        {:source-paths ["src"]
                         :compiler {:output-to "out/dev/app.js"
                                    :optimizations :none
                                    :source-map true}}
                       :prod
                        {:source-paths ["src"]
                         :compiler {:output-to "out/prod/app.js"
                                    :optimizations :advanced
                                    :pretty-print false}}}})
