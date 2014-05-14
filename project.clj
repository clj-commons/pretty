(defproject io.aviso/pretty "0.1.12-SNAPSHOT"
  :description "Clojure library to help print things, prettily"
  :url "https://github.com/AvisoNovate/pretty"
  :license {:name "Apache Sofware License 2.0"
            :url  "http://www.apache.org/licenses/LICENSE-2.0.html"}
  ;; Normally we don't AOT compile; only when tracking down reflection warnings.
  :dependencies [[org.clojure/clojure "1.6.0"]]
  :codox {:src-dir-uri               "https://github.com/AvisoNovate/pretty/blob/master/"
          :src-linenum-anchor-prefix "L"}
  )
