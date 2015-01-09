(defproject io.aviso/pretty "0.1.14"
            :description "Clojure library to help print things, prettily"
            :url "https://github.com/AvisoNovate/pretty"
            :license {:name "Apache Sofware License 2.0"
                      :url  "http://www.apache.org/licenses/LICENSE-2.0.html"}
            ;; Normally we don't AOT compile; only when tracking down reflection warnings.
            :dependencies [[org.clojure/clojure "1.6.0"]]
            :plugins [[lein-shell "0.4.0"]]
            :shell {:commands {"scp" {:dir "doc"}}}
            :aliases {"deploy-doc" ["shell"
                                    "scp" "-r" "." "hlship_howardlewisship@ssh.phx.nearlyfreespeech.net:io.aviso/pretty"]
                      "release"    ["do"
                                    "clean,"
                                    "doc,"
                                    "deploy-doc,"
                                    "deploy" "clojars"]}
            :codox {:src-dir-uri               "https://github.com/AvisoNovate/pretty/blob/master/"
                    :src-linenum-anchor-prefix "L"
                    :defaults                  {:doc/format :markdown}})
