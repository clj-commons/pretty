;; clj -T:build <var>

(ns build
  (:require [clojure.tools.build.api :as build]
            [net.lewisship.build :as b]
            [clojure.string :as str]))

(def lib 'io.aviso/pretty)
(def version (-> "VERSION.txt" slurp str/trim))

(def jar-params {:project-name lib
                 :version version})

(defn clean
  [_params]
  (build/delete {:path "target"}))

(defn jar
  [_params]
  (b/create-jar jar-params))

(defn deploy
  [_params]
  (clean nil)
  (b/deploy-jar (jar nil)))

(defn codox
  [_params]
  (b/generate-codox {:project-name lib
                     :version version
                     :aliases [:dev]}))

(def publish-dir "../aviso-docs/pretty")

(defn publish
  "Generate Codox documentation and publish via a GitHub push."
  [_params]
  (println "Generating Codox documentation ...")
  (codox nil)
  (println "Copying documentation to" publish-dir "...")
  (build/copy-dir {:target-dir publish-dir
                   :src-dirs ["target/doc"]})
  (println "Committing changes ...")
  (build/process {:dir publish-dir
                  :command-args ["git" "commit" "-a" "-m" (str "io.aviso/pretty " version)]})
  (println "Pushing changes ...")
  (build/process {:dir publish-dir
                  :command-args ["git" "push"]}))
