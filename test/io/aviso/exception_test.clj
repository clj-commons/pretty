(ns io.aviso.exception-test
  (use clojure.test)
  (:require [clojure.string :as str]
            [io.aviso.exception :refer [*fonts* parse-exception]]
            [clojure.pprint :refer [pprint]]))


(defn parse [text-lines]
  (let [text (str/join \newline text-lines)]
    (binding [*fonts* nil]
      (parse-exception text nil))))

(deftest parse-exceptions
  (is (= [{:class-name "java.lang.IllegalArgumentException"
           :message    "No value supplied for key: {:host \"example.com\"}"
           :stack-trace
                       [{:simple-class   "PersistentHashMap"
                         :package        "clojure.lang"
                         :omitted        true
                         :is-clojure?    false
                         :method         "create"
                         :name           ""
                         :formatted-name "..."
                         :file           ""
                         :line           nil
                         :class          "clojure.lang.PersistentHashMap"
                         :names          []}
                        {:simple-class   "client$tcp_client"
                         :package        "riemann"
                         :is-clojure?    true
                         :method         "doInvoke"
                         :name           "riemann.client/tcp-client"
                         :formatted-name "riemann.client/tcp-client"
                         :file           "client.clj"
                         :line           90
                         :class          "riemann.client$tcp_client"
                         :names          '("riemann.client" "tcp-client")}
                        {:simple-class   "RestFn"
                         :package        "clojure.lang"
                         :omitted        true
                         :is-clojure?    false
                         :method         "invoke"
                         :name           ""
                         :formatted-name "..."
                         :file           ""
                         :line           nil
                         :class          "clojure.lang.RestFn"
                         :names          []}
                        {:simple-class   "error_monitor$make_connection"
                         :package        "com.example"
                         :is-clojure?    true
                         :method         "invoke"
                         :name           "com.example.error-monitor/make-connection"
                         :formatted-name "com.example.error-monitor/make-connection"
                         :file           "error_monitor.clj"
                         :line           22
                         :class          "com.example.error_monitor$make_connection"
                         :names          '("com.example.error-monitor" "make-connection")}
                        {:simple-class   "error_monitor$make_client"
                         :package        "com.example"
                         :is-clojure?    true
                         :method         "invoke"
                         :name           "com.example.error-monitor/make-client"
                         :formatted-name "com.example.error-monitor/make-client"
                         :file           "error_monitor.clj"
                         :line           26
                         :class          "com.example.error_monitor$make_client"
                         :names          '("com.example.error-monitor" "make-client")}
                        {:simple-class   "core$map$fn__4553"
                         :package        "clojure"
                         :is-clojure?    true
                         :method         "invoke"
                         :name           "clojure.core/map/fn"
                         :formatted-name "clojure.core/map/fn"
                         :file           "core.clj"
                         :line           2624
                         :class          "clojure.core$map$fn__4553"
                         :names          '("clojure.core" "map" "fn")}
                        {:simple-class   "LazySeq"
                         :package        "clojure.lang"
                         :omitted        true
                         :is-clojure?    false
                         :method         "sval"
                         :name           ""
                         :formatted-name "..."
                         :file           ""
                         :line           nil
                         :class          "clojure.lang.LazySeq"
                         :names          []}
                        {:simple-class   "core$seq__4128"
                         :package        "clojure"
                         :is-clojure?    true
                         :method         "invoke"
                         :name           "clojure.core/seq"
                         :formatted-name "clojure.core/seq"
                         :file           "core.clj"
                         :line           137
                         :class          "clojure.core$seq__4128"
                         :names          '("clojure.core" "seq")}
                        {:simple-class   "core$sort"
                         :package        "clojure"
                         :is-clojure?    true
                         :method         "invoke"
                         :name           "clojure.core/sort"
                         :formatted-name "clojure.core/sort"
                         :file           "core.clj"
                         :line           2981
                         :class          "clojure.core$sort"
                         :names          '("clojure.core" "sort")}
                        {:simple-class   "core$sort_by"
                         :package        "clojure"
                         :is-clojure?    true
                         :method         "invoke"
                         :name           "clojure.core/sort-by"
                         :formatted-name "clojure.core/sort-by"
                         :file           "core.clj"
                         :line           2998
                         :class          "clojure.core$sort_by"
                         :names          '("clojure.core" "sort-by")}
                        {:simple-class   "core$sort_by"
                         :package        "clojure"
                         :is-clojure?    true
                         :method         "invoke"
                         :name           "clojure.core/sort-by"
                         :formatted-name "clojure.core/sort-by"
                         :file           "core.clj"
                         :line           2996
                         :class          "clojure.core$sort_by"
                         :names          '("clojure.core" "sort-by")}
                        {:simple-class   "error_monitor$make_clients"
                         :package        "com.example"
                         :is-clojure?    true
                         :method         "invoke"
                         :name           "com.example.error-monitor/make-clients"
                         :formatted-name "com.example.error-monitor/make-clients"
                         :file           "error_monitor.clj"
                         :line           31
                         :class          "com.example.error_monitor$make_clients"
                         :names          '("com.example.error-monitor" "make-clients")}
                        {:simple-class   "error_monitor$report_and_reset"
                         :package        "com.example"
                         :is-clojure?    true
                         :method         "invoke"
                         :name           "com.example.error-monitor/report-and-reset"
                         :formatted-name "com.example.error-monitor/report-and-reset"
                         :file           "error_monitor.clj"
                         :line           185
                         :class          "com.example.error_monitor$report_and_reset"
                         :names          '("com.example.error-monitor" "report-and-reset")}
                        {:simple-class   "main$_main$fn__705"
                         :package        "com.example.error_monitor"
                         :is-clojure?    true
                         :method         "invoke"
                         :name           "com.example.error-monitor.main/-main/fn"
                         :formatted-name "com.example.error-monitor.main/-main/fn"
                         :file           "main.clj"
                         :line           19
                         :class          "com.example.error_monitor.main$_main$fn__705"
                         :names          '("com.example.error-monitor.main" "-main" "fn")}
                        {:simple-class   "main$_main"
                         :package        "com.example.error_monitor"
                         :is-clojure?    true
                         :method         "doInvoke"
                         :name           "com.example.error-monitor.main/-main"
                         :formatted-name "com.example.error-monitor.main/-main"
                         :file           "main.clj"
                         :line           16
                         :class          "com.example.error_monitor.main$_main"
                         :names          '("com.example.error-monitor.main" "-main")}
                        {:simple-class   "RestFn"
                         :package        "clojure.lang"
                         :omitted        true
                         :is-clojure?    false
                         :method         "applyTo"
                         :name           ""
                         :formatted-name "..."
                         :file           ""
                         :line           nil
                         :class          "clojure.lang.RestFn"
                         :names          []}
                        {:class          "com.example.error_monitor.main"
                         :file           ""
                         :formatted-name "com.example.error_monitor.main.main"
                         :is-clojure?    false
                         :line           nil
                         :method         "main"
                         :name           ""
                         :names          []
                         :package        "com.example.error_monitor"
                         :simple-class   "main"}]}]
         (parse ["java.lang.IllegalArgumentException: No value supplied for key: {:host \"example.com\"}"
                 "\tat clojure.lang.PersistentHashMap.create(PersistentHashMap.java:77)"
                 "\tat riemann.client$tcp_client.doInvoke(client.clj:90)"
                 "\tat clojure.lang.RestFn.invoke(RestFn.java:408)"
                 "\tat com.example.error_monitor$make_connection.invoke(error_monitor.clj:22)"
                 "\tat com.example.error_monitor$make_client.invoke(error_monitor.clj:26)"
                 "\tat clojure.core$map$fn__4553.invoke(core.clj:2624)"
                 "\tat clojure.lang.LazySeq.sval(LazySeq.java:40)"
                 "\tat clojure.lang.LazySeq.seq(LazySeq.java:49)"
                 "\tat clojure.lang.RT.seq(RT.java:507)"
                 "\tat clojure.core$seq__4128.invoke(core.clj:137)"
                 "\tat clojure.core$sort.invoke(core.clj:2981)"
                 "\tat clojure.core$sort_by.invoke(core.clj:2998)"
                 "\tat clojure.core$sort_by.invoke(core.clj:2996)"
                 "\tat com.example.error_monitor$make_clients.invoke(error_monitor.clj:31)"
                 "\tat com.example.error_monitor$report_and_reset.invoke(error_monitor.clj:185)"
                 "\tat com.example.error_monitor.main$_main$fn__705.invoke(main.clj:19)"
                 "\tat com.example.error_monitor.main$_main.doInvoke(main.clj:16)"
                 "\tat clojure.lang.RestFn.applyTo(RestFn.java:137)"
                 "\tat com.example.error_monitor.main.main(Unknown Source)"]))))
