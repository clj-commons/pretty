(defproject io.aviso/pretty "0.1.37"
  :description "Clojure library to help print things, prettily"
  :url "https://github.com/AvisoNovate/pretty"
  :license {:name "Apache Sofware License 2.0"
            :url  "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/tools.logging "0.4.1" :optional true]]
  :plugins [[lein-codox "0.10.4"]]
  :profiles {:1.9  {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :1.8  {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :dev  {:dependencies [[criterium "0.4.4"]
                                   [com.stuartsierra/component "0.3.2"]
                                   [com.walmartlabs/test-reporting "0.1.0"]]}
             :lein {:dependencies [[leiningen "2.8.2"]]}}
  ;; Part of release is currently manual; copy target/docs to the AvisoNovate/docs/pretty folder
  :aliases {"release" ["do"
                       "clean,"
                       "deploy" "clojars"]`
            "docs"    ["with-profiles" "+lein,+dev" "codox"]}
  :codox {:source-uri "https://github.com/AvisoNovate/pretty/blob/master/{filepath}#L{line}"
          :metadata   {:doc/format :markdown}})
