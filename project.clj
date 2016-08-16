(defproject io.aviso/pretty "0.1.31-SNAPSHOT"
  :description "Clojure library to help print things, prettily"
  :url "https://github.com/AvisoNovate/pretty"
  :license {:name "Apache Sofware License 2.0"
            :url  "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [[org.clojure/clojure "1.5.0"]
                 [org.clojure/tools.logging "0.3.1" :optional true]]
  :plugins [[lein-codox "0.9.3"]]
  :profiles {:1.7  {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.8  {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :1.9  {:dependencies [[org.clojure/clojure "1.9.0-alpha10"]]}
             :dev  {:dependencies [[criterium "0.4.4"]]}
             :lein {:dependencies [[leiningen "2.6.1"]]}}
  ;; Part of release is currently manual; copy target/docs to the AvisoNovate/docs/pretty folder
  :aliases {"release" ["do"
                       "clean,"
                       "deploy" "clojars"]}
  :codox {:source-uri "https://github.com/AvisoNovate/pretty/blob/master/{filepath}#L{line}"
          :metadata   {:doc/format :markdown}})
