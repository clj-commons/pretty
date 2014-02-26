(defproject io.aviso/pretty "0.1.20-SNAPSHOT"
  :description "Clojure library to help print things, prettily"
  :url "https://github.com/AvisoNovate/pretty"
  :license {:name "Apache Sofware License 2.0"
            :url  "http://www.apache.org/licenses/LICENSE-2.0.html"}
  ;; Normally we don't AOT compile; only when tracking down reflection warnings.
  :profiles {:reflection-warnings {:aot         :all
                                   :global-vars {*warn-on-reflection* true}}}
  :dependencies [[org.clojure/clojure "1.5.1"]]
  :codox {:src-dir-uri               "https://github.com/AvisoNovate/pretty/blob/master/"
          :src-linenum-anchor-prefix "L"}
  )
