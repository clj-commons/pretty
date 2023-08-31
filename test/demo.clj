(ns demo
  (:require
    [clj-commons.pretty.repl :as repl]
    [clj-commons.format.exceptions :as e]
    [clj-commons.ansi :refer [compose pcompose]]
    [clj-commons.format.binary :as b]
    [clojure.java.io :as io]
    [clojure.repl :refer [pst]]
    [criterium.core :as c]
    [clojure.test :refer [report deftest is]])
  (:import
    (java.nio.file Files)
    (java.sql SQLException)))

(defn- jdbc-update
  []
  (throw (SQLException. "Database failure\nSELECT FOO, BAR, BAZ\nFROM GNIP\nfailed with ABC123" "ABC" 123)))

(defprotocol Worker
  (do-work [this]))

(defn make-jdbc-update-worker
  []
  (reify Worker
    (do-work [_this] (jdbc-update))))

(defn- update-row
  []
  (try
    (-> (make-jdbc-update-worker) do-work)
    (catch Throwable e
      (throw (RuntimeException. "Failure updating row" e)))))

(defn make-exception
  "Creates a sample exception used to test the exception formatting logic."
  []
  (try
    (update-row)
    (catch Throwable e
      ;; Return it, not rethrow it.
      (RuntimeException. "Request handling exception" e))))

(defn make-ex-info
  ""
  []
  (try
    (throw (make-exception))
    (catch Throwable t
      ;; Return it, not rethrow it.
      (ex-info "Exception in make-ex-info."
               {:function 'make-exception}
               t))))

(defn infinite-loop
  []
  (infinite-loop))

(defn countdown
  [n]
  (if (zero? n)
    (throw (RuntimeException. "Boom!"))
    (countdown (dec n))))

(defn test-failure
  []
  (report {:type :error :expected nil :actual (make-ex-info)}))

(defn -main [& args]
  (prn `-main :args args)
  (println "Clojure version: " *clojure-version*)
  (println "Installing pretty exceptions ...")
  (repl/install-pretty-exceptions)
  (pcompose [:bold.green "ok"])
  (pst (make-exception))
  (println "\nTesting reporting of repeats:")
  (try (countdown 20)
       (catch Throwable t (e/print-exception t)))
  (println "\nBinary output:\n")
  (-> (io/file "test/tiny-clojure.gif")
      .toPath
      Files/readAllBytes
      (b/print-binary {:ascii true}))

  (println "\nBinary delta:\n")
  (b/print-binary-delta "Welcome, Friend"
                        "We1come, Fiend")
  (println))

(deftest fail-wrong-exception
  (is (thrown? IllegalArgumentException
               (jdbc-update))))

(deftest error-thrown-exception
  (jdbc-update))

(deftest fail-wrong-message
  (is (thrown-with-msg? SQLException #"validation failure"
                        (jdbc-update))))

(comment

  (require '[clojure.core.async :refer [chan <!! close! thread]])

  (let [e (make-ex-info)
        ch (chan)]
    (dotimes [i 5]
      (thread
        (<!! ch)
        (println (str "Thread #" i))
        (e/print-exception e)))
    (close! ch))


  (clojure.test/run-tests)

  (repl/install-pretty-exceptions)

  (countdown 10)
  (infinite-loop)
  (throw (make-ex-info))
  (print (make-ex-info))
  (test-failure)
  (e/print-exception (Throwable. "hello"))

  *clojure-version*
  (str (Runtime/version))

  ;; 11 Feb 2016 -   553 µs (14 µs std dev) - Clojure 1.8
  ;; 13 Sep 2021 -   401 µs (16 µs std dev) - Clojure 1.11.1
  ;; 20 Jun 2023 -   713 µs (30 µs std dev) - Clojure 1.11.1, Corretto 17.0.7, M1
  ;; 25 Jun 2023 -   507 µs                 - Clojure 1.11.1, Corretto 17.0.7, M1
  ;; 26 Jun 2023 -  1.13 ms                 - Clojure 1.11.1, Corretto 17.0.7, M1
  ;; 11 Aug 2023 -  1.09 ms                 - Clojure 1.11.1, Corretto 17.0.4, M1
  (let [e (make-ex-info)]
    (c/bench (e/format-exception e)))


  ;; 27 Jun 2023 -  767 µs                  - Clojure 1.11.1, Corretto 17.0.7, M1
  (let [e (make-ex-info)
        composed (#'e/render-exception (e/analyze-exception e nil) nil)]
    (c/bench (compose composed)))

  ;; 27 Jun 2023 - 182 µs                   - Clojure 1.11.1, Corretto 17.0.7, M1
  (let [e (make-ex-info)]
    (c/bench (#'e/render-exception (e/analyze-exception e nil) nil)))

  ;; 11 Feb 2016 - 213 µs (4 µs std dev) - Clojure 1.8
  ;; 28 Sep 2018 - 237 µs (8 µs std dev) - Clojure 1.9

  (let [e (make-ex-info)]
    (c/bench (doseq [x (e/analyze-exception e nil)]
               (-> x :stack-trace doall))))
  )
