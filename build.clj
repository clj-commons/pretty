;; clj -T:build <var>

(ns build
  (:require [clojure.set :as set]
            [clojure.tools.build.api :as build]
            [net.lewisship.build :as b]
            [clojure.set :as set]
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

(defn install
  [_params]
  (build/install (-> (jar nil)
                     (assoc :lib lib)
                     (set/rename-keys {:jar-path :jar-file}))))

(defn deploy
  [_params]
  (clean nil)
  (b/deploy-jar (assoc (jar nil) :sign-artifacts? false)))

(defn codox
  [_params]
  (b/generate-codox {:project-name lib
                     :version version}))

