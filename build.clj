;; clj -T:build <var>

(ns build
  (:require [clojure.tools.build.api :as build]
            [net.lewisship.build :as b]
            [clojure.string :as str]))

(def lib 'org.clj-commons/pretty)
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
  (b/deploy-jar (assoc (jar nil) :sign-artifacts? false)))

(defn codox
  [_params]
  (b/generate-codox {:project-name lib
                     :version version}))

