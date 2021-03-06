(defproject pmmt "0.1.0-SNAPSHOT"

  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[bouncer "1.0.0"]
                 [buddy "1.1.0"]
                 [clj-pdf "2.2.29"]
                 [clj-time "0.12.0"]
                 [cljs-ajax "0.5.8"]
                 [cljsjs/plotly "1.29.3-0"]
                 [cljsjs/google-maps "3.18-1"]
                 [cljsjs/jquery "3.2.1-0"]
                 [com.andrewmcveigh/cljs-time "0.4.0"]
                 [com.cognitect/transit-java "0.8.337"]
                 [compojure "1.6.1"]
                 [conman "0.6.7"]
                 [cprop "0.1.13"]
                 [day8.re-frame/http-fx "0.1.2"]
                 [laconic/utils "0.1.0-SNAPSHOT"]
                 [luminus-immutant "0.2.2"]
                 [luminus-nrepl "0.1.4"]
                 [luminus-migrations "0.2.7"]
                 [markdown-clj "0.9.89"]
                 [metosin/ring-http-response "0.8.0"]
                 [metosin/compojure-api "1.1.8"]
                 [mount "0.1.15"]
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.339"]
                 [org.clojure/data.csv "0.1.3"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.postgresql/postgresql "9.4.1211"]
                 [org.webjars/bootstrap "4.0.0-alpha.3"]
                 [org.webjars.bower/tether "1.3.7"]
                 [org.webjars/font-awesome "4.6.3"]
                 [org.webjars/webjars-locator-jboss-vfs "0.1.0"]
                 [prismatic/dommy "1.1.0"]
                 [reagent-utils "0.2.0"]
                 [reagent "0.8.1"]
                 [reagent-forms "0.5.29"]
                 [re-frame "0.10.2"]
                 [reframe-forms "0.1.0-SNAPSHOT"]
                 [ring-middleware-format "0.7.0"]
                 [ring-webjars "0.1.1"]
                 [ring/ring-defaults "0.2.1"]
                 [secretary "1.2.3"]
                 [selmer "1.0.9"]
                 [venantius/accountant "0.1.7"]]




  :min-lein-version "2.0.0"

  :jvm-opts ["-server" "-Dconf=.lein-env"]
  :source-paths ["src/clj" "src/cljc"]
  :resource-paths ["resources" "target/cljsbuild"]
  :target-path "target/%s/"
  :main pmmt.core
  :migratus {:store :database :db ~(get (System/getenv) "DATABASE_URL")}

  :plugins [[lein-cprop "1.0.1"]
            [migratus-lein "0.4.2"]
            [lein-cljsbuild "1.1.4"]
            [lein-immutant "2.1.0"]
            [lein-exec "0.3.6"]]
  :clean-targets ^{:protect false}
  [:target-path [:cljsbuild :builds :app :compiler :output-dir] [:cljsbuild :builds :app :compiler :output-to]]
  :figwheel
  {:http-server-root "public"
   :nrepl-port 7002
   :css-dirs ["resources/public/css"]
   :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}


  :profiles
  {:uberjar {:omit-source true
             :prep-tasks ["compile" ["cljsbuild" "once" "min"]]
             :cljsbuild
             {:builds
              {:min
               {:source-paths ["src/cljc" "src/cljs" "env/prod/cljs"]
                :compiler
                {:output-to "target/cljsbuild/public/js/app.js"
                 :externs ["react/externs/react.js", "resources/externs.js"]
                 :optimizations :advanced
                 :pretty-print true
                 :pseudo-names true
                 :closure-warnings
                 {:externs-validation :off :non-standard-jsdoc :off}}}}}


             :aot :all
             :uberjar-name "pmmt.jar"
             :source-paths ["env/prod/clj"]
             :resource-paths ["env/prod/resources"]}

   :dev           [:project/dev :profiles/dev]
   :test          [:project/dev :project/test :profiles/test]

   :project/dev  {:dependencies [[prone "1.1.2"]
                                 [ring/ring-mock "0.3.0"]
                                 [ring/ring-devel "1.5.0"]
                                 [pjstadig/humane-test-output "0.8.1"]
                                 [doo "0.1.7"]
                                 [binaryage/devtools "0.9.10"]
                                 [figwheel-sidecar "0.5.8"]
                                 [com.cemerick/piggieback "0.2.2-SNAPSHOT"]
                                 [org.clojure/tools.trace "0.7.9"]]
                  :plugins      [[com.jakemccrary/lein-test-refresh "0.14.0"]
                                 [lein-doo "0.1.7"]
                                 [lein-figwheel "0.5.16"]
                                 [org.clojure/clojurescript "1.9.229"]]
                  :cljsbuild
                  {:builds
                   {:app
                    {:source-paths ["src/cljs" "src/cljc" "env/dev/cljs"]
                     :compiler
                     {:main "pmmt.app"
                      :asset-path "/js/out"
                      :output-to "target/cljsbuild/public/js/app.js"
                      :output-dir "target/cljsbuild/public/js/out"
                      :source-map true
                      :optimizations :none
                      :pretty-print true}}}}



                  :doo {:build "test"}
                  :source-paths ["env/dev/clj" "test/clj"]
                  :resource-paths ["env/dev/resources"]
                  :repl-options {:init-ns user
                                 :timeout 120000}
                  :injections [(require 'pjstadig.humane-test-output)
                               (pjstadig.humane-test-output/activate!)]}
   :project/test {:resource-paths ["env/dev/resources" "env/test/resources"]
                  :cljsbuild
                  {:builds
                   {:test
                    {:source-paths ["src/cljc" "src/cljs" "test/cljs"]
                     :compiler
                     {:output-to "target/test.js"
                      :main "pmmt.doo-runner"
                      :optimizations :whitespace
                      :pretty-print true}}}}}


   :profiles/dev {}
   :profiles/test {}})
