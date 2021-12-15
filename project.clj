(defproject io.aviso/pretty "1.1.1"
  :description "Clojure library to help print things, prettily"
  :url "https://github.com/AvisoNovate/pretty"
  :license {:name "Apache Software License 2.0"
            :url  "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.logging "1.2.2" :optional true]]
  :plugins [[lein-codox "0.10.7"]]
  :profiles {:1.9  {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :1.8  {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :dev  {:dependencies [[criterium "0.4.6"]
                                   [com.stuartsierra/component "1.0.0"]
                                   [com.walmartlabs/test-reporting "1.1"]]}
             :lein {:dependencies [[leiningen "2.9.8"]]}}
  :aliases {"release" ["do"
                       "clean,"
                       "deploy" "clojars"]`
            "docs"    ["with-profiles" "+1.9,+lein,+dev" "codox"]}
  :codox {:source-uri "https://github.com/AvisoNovate/pretty/blob/master/{filepath}#L{line}"
          :metadata   {:doc/format :markdown}})
