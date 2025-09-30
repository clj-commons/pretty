(ns clj-commons.exception-test
  (:require [clj-commons.test-common :as tc]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.test :refer [deftest is use-fixtures testing]]
            [matcher-combinators.matchers :as m]
            [clj-commons.ansi :refer [*color-enabled*]]
            [clj-commons.pretty-impl :refer [csi]]
            [clj-commons.format.exceptions :as f :refer [*fonts* parse-exception format-exception]]))

(use-fixtures :once tc/spec-fixture)

(deftest write-exceptions
  (testing "exception properties printing"
    (testing "Does not fail with ex-info's map keys not implementing clojure.lang.Named"
      (is (re-find #"string-key.*string-val"
                   (format-exception (ex-info "Error" {"string-key" "string-val"})))))))

(defn countdown
  [n]
  (if (zero? n)
    (throw (RuntimeException. "Boom!"))
    (countdown (dec n))))

(deftest captures-repeat-counts
  (binding [*fonts* nil]
    (let [formatted (try (countdown 20)
                         (catch Throwable t
                           (format-exception t)))]
      (is (re-find #"(?xmd) \Qclj-commons.exception-test/countdown\E (.*) \Q(repeats 20 times)\E"
                      formatted)))))

(deftest binding-fonts-to-nil-is-same-as-no-color
  (let [ex (ex-info "does not matter" {:gnip :gnop})
        with-fonts-but-no-color (binding [*color-enabled* false]
                                  (format-exception ex))
        with-color-but-no-fonts (binding [*fonts* nil]
                                  (format-exception ex))]
    (is (= with-fonts-but-no-color
           with-color-but-no-fonts))

    (is (not (string/includes? with-fonts-but-no-color csi)))))

(defn parse [& text-lines]
  (let [text (string/join \newline text-lines)]
    (parse-exception text nil)))

(deftest parse-exceptions
  (is (= [{:class-name "java.lang.IllegalArgumentException"
           :message "No value supplied for key: {:host \"example.com\"}"
           :stack-trace [{:class "clojure.lang.PersistentHashMap"
                          :file "PersistentHashMap.java"
                          :id "clojure.lang.PersistentHashMap.create:77"
                          :is-clojure? false
                          :line 77
                          :method "create"
                          :name ""
                          :names []
                          :omitted true
                          :package "clojure.lang"
                          :simple-class "PersistentHashMap"}
                         {:class "riemann.client$tcp_client"
                          :file "client.clj"
                          :id "riemann.client/tcp-client:90"
                          :is-clojure? true
                          :line 90
                          :method "doInvoke"
                          :name "riemann.client/tcp-client"
                          :names ["riemann.client"
                                  "tcp-client"]
                          :package "riemann"
                          :simple-class "client$tcp_client"}
                         {:class "com.example.error_monitor$make_connection"
                          :file "error_monitor.clj"
                          :id "com.example.error-monitor/make-connection:22"
                          :is-clojure? true
                          :line 22
                          :method "invoke"
                          :name "com.example.error-monitor/make-connection"
                          :names ["com.example.error-monitor"
                                  "make-connection"]
                          :package "com.example"
                          :simple-class "error_monitor$make_connection"}
                         {:class "com.example.error_monitor$make_client"
                          :file "error_monitor.clj"
                          :id "com.example.error-monitor/make-client:26"
                          :is-clojure? true
                          :line 26
                          :method "invoke"
                          :name "com.example.error-monitor/make-client"
                          :names ["com.example.error-monitor"
                                  "make-client"]
                          :package "com.example"
                          :simple-class "error_monitor$make_client"}
                         {:class "clojure.core$map$fn__4553"
                          :file "core.clj"
                          :id "clojure.core/map/fn:2624"
                          :is-clojure? true
                          :line 2624
                          :method "invoke"
                          :name "clojure.core/map/fn"
                          :names ["clojure.core"
                                  "map"
                                  "fn"]
                          :package "clojure"
                          :simple-class "core$map$fn__4553"}
                         {:class "clojure.lang.LazySeq"
                          :file "LazySeq.java"
                          :id "clojure.lang.LazySeq.sval:40"
                          :is-clojure? false
                          :line 40
                          :method "sval"
                          :name ""
                          :names []
                          :omitted true
                          :package "clojure.lang"
                          :simple-class "LazySeq"}
                         {:class "clojure.core$seq__4128"
                          :file "core.clj"
                          :id "clojure.core/seq:137"
                          :is-clojure? true
                          :line 137
                          :method "invoke"
                          :name "clojure.core/seq"
                          :names ["clojure.core"
                                  "seq"]
                          :package "clojure"
                          :simple-class "core$seq__4128"}
                         {:class "clojure.core$sort"
                          :file "core.clj"
                          :id "clojure.core/sort:2981"
                          :is-clojure? true
                          :line 2981
                          :method "invoke"
                          :name "clojure.core/sort"
                          :names ["clojure.core"
                                  "sort"]
                          :package "clojure"
                          :simple-class "core$sort"}
                         {:class "clojure.core$sort_by"
                          :file "core.clj"
                          :id "clojure.core/sort-by:2998"
                          :is-clojure? true
                          :line 2998
                          :method "invoke"
                          :name "clojure.core/sort-by"
                          :names ["clojure.core"
                                  "sort-by"]
                          :package "clojure"
                          :simple-class "core$sort_by"}
                         {:class "clojure.core$sort_by"
                          :file "core.clj"
                          :id "clojure.core/sort-by:2996"
                          :is-clojure? true
                          :line 2996
                          :method "invoke"
                          :name "clojure.core/sort-by"
                          :names ["clojure.core"
                                  "sort-by"]
                          :package "clojure"
                          :simple-class "core$sort_by"}
                         {:class "com.example.error_monitor$make_clients"
                          :file "error_monitor.clj"
                          :id "com.example.error-monitor/make-clients:31"
                          :is-clojure? true
                          :line 31
                          :method "invoke"
                          :name "com.example.error-monitor/make-clients"
                          :names ["com.example.error-monitor"
                                  "make-clients"]
                          :package "com.example"
                          :simple-class "error_monitor$make_clients"}
                         {:class "com.example.error_monitor$report_and_reset"
                          :file "error_monitor.clj"
                          :id "com.example.error-monitor/report-and-reset:185"
                          :is-clojure? true
                          :line 185
                          :method "invoke"
                          :name "com.example.error-monitor/report-and-reset"
                          :names ["com.example.error-monitor"
                                  "report-and-reset"]
                          :package "com.example"
                          :simple-class "error_monitor$report_and_reset"}
                         {:class "com.example.error_monitor.main$_main$fn__705"
                          :file "main.clj"
                          :id "com.example.error-monitor.main/-main/fn:19"
                          :is-clojure? true
                          :line 19
                          :method "invoke"
                          :name "com.example.error-monitor.main/-main/fn"
                          :names ["com.example.error-monitor.main"
                                  "-main"
                                  "fn"]
                          :package "com.example.error_monitor"
                          :simple-class "main$_main$fn__705"}
                         {:class "com.example.error_monitor.main$_main"
                          :file "main.clj"
                          :id "com.example.error-monitor.main/-main:16"
                          :is-clojure? true
                          :line 16
                          :method "doInvoke"
                          :name "com.example.error-monitor.main/-main"
                          :names ["com.example.error-monitor.main"
                                  "-main"]
                          :package "com.example.error_monitor"
                          :simple-class "main$_main"}
                         {:class "com.example.error_monitor.main"
                          :file ""
                          :id "com.example.error_monitor.main.main:-1"
                          :is-clojure? false
                          :line nil
                          :method "main"
                          :name ""
                          :names []
                          :package "com.example.error_monitor"
                          :simple-class "main"}]}]
         (parse "java.lang.IllegalArgumentException: No value supplied for key: {:host \"example.com\"}"
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
                "\tat com.example.error_monitor.main.main(Unknown Source)")))

  (is (= [{:class-name "java.lang.RuntimeException"
           :message "Request handling exception"}
          {:class-name "java.lang.RuntimeException"
           :message "Failure updating row"}
          {:class-name "java.sql.SQLException"
           :message "Database failure
SELECT FOO, BAR, BAZ
FROM GNIP
failed with ABC123"
           :stack-trace [{:class "user$jdbc_update"
                          :file "user.clj"
                          :id "user/jdbc-update:7"
                          :is-clojure? true
                          :line 7
                          :method "invoke"
                          :name "user/jdbc-update"
                          :names ["user"
                                  "jdbc-update"]
                          :package nil
                          :simple-class "user$jdbc_update"}
                         {:class "user$make_jdbc_update_worker$reify__497"
                          :file "user.clj"
                          :id "user/make-jdbc-update-worker/reify/do-work:18"
                          :is-clojure? true
                          :line 18
                          :method "do_work"
                          :name "user/make-jdbc-update-worker/reify/do-work"
                          :names ["user"
                                  "make-jdbc-update-worker"
                                  "reify"
                                  "do-work"]
                          :package nil
                          :simple-class "user$make_jdbc_update_worker$reify__497"}
                         {:class "user$update_row"
                          :file "user.clj"
                          :id "user/update-row:23"
                          :is-clojure? true
                          :line 23
                          :method "invoke"
                          :name "user/update-row"
                          :names ["user"
                                  "update-row"]
                          :package nil
                          :simple-class "user$update_row"}
                         {:class "user$make_exception"
                          :file "user.clj"
                          :id "user/make-exception:31"
                          :is-clojure? true
                          :line 31
                          :method "invoke"
                          :name "user/make-exception"
                          :names ["user"
                                  "make-exception"]
                          :package nil
                          :simple-class "user$make_exception"}
                         {:class "user$eval2018"
                          :file "REPL Input"
                          :id "user/eval2018"
                          :is-clojure? true
                          :line nil
                          :method "invoke"
                          :name "user/eval2018"
                          :names ["user"
                                  "eval2018"]
                          :package nil
                          :simple-class "user$eval2018"}
                         {:class "clojure.lang.Compiler"
                          :file "Compiler.java"
                          :id "clojure.lang.Compiler.eval:6619"
                          :is-clojure? false
                          :line 6619
                          :method "eval"
                          :name ""
                          :names []
                          :omitted true
                          :package "clojure.lang"
                          :simple-class "Compiler"}
                         {:class "clojure.core$eval"
                          :file "core.clj"
                          :id "clojure.core/eval:2852"
                          :is-clojure? true
                          :line 2852
                          :method "invoke"
                          :name "clojure.core/eval"
                          :names ["clojure.core"
                                  "eval"]
                          :package "clojure"
                          :simple-class "core$eval"}]}]
         (parse "java.lang.RuntimeException: Request handling exception"
                "\tat user$make_exception.invoke(user.clj:31)"
                "\tat user$eval2018.invoke(form-init1482095333541107022.clj:1)"
                "\tat clojure.lang.Compiler.eval(Compiler.java:6619)"
                "\tat clojure.lang.Compiler.eval(Compiler.java:6582)"
                "\tat clojure.core$eval.invoke(core.clj:2852)"
                "\tat clojure.main$repl$read_eval_print__6602$fn__6605.invoke(main.clj:259)"
                "\tat clojure.main$repl$read_eval_print__6602.invoke(main.clj:259)"
                "\tat clojure.main$repl$fn__6611$fn__6612.invoke(main.clj:277)"
                "\tat clojure.main$repl$fn__6611.invoke(main.clj:277)"
                "\tat clojure.main$repl.doInvoke(main.clj:275)"
                "\tat clojure.lang.RestFn.invoke(RestFn.java:1523)"
                "\tat clojure.tools.nrepl.middleware.interruptible_eval$evaluate$fn__1419.invoke(interruptible_eval.clj:72)"
                "\tat clojure.lang.AFn.applyToHelper(AFn.java:159)"
                "\tat clojure.lang.AFn.applyTo(AFn.java:151)"
                "\tat clojure.core$apply.invoke(core.clj:617)"
                "\tat clojure.core$with_bindings_STAR_.doInvoke(core.clj:1788)"
                "\tat clojure.lang.RestFn.invoke(RestFn.java:425)"
                "\tat clojure.tools.nrepl.middleware.interruptible_eval$evaluate.invoke(interruptible_eval.clj:56)"
                "\tat clojure.tools.nrepl.middleware.interruptible_eval$interruptible_eval$fn__1461$fn__1464.invoke(interruptible_eval.clj:191)"
                "\tat clojure.tools.nrepl.middleware.interruptible_eval$run_next$fn__1456.invoke(interruptible_eval.clj:159)"
                "\tat clojure.lang.AFn.run(AFn.java:24)"
                "\tat java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)"
                "\tat java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)"
                "\tat java.lang.Thread.run(Thread.java:745)"
                "Caused by: java.lang.RuntimeException: Failure updating row"
                "\tat user$update_row.invoke(user.clj:23)"
                "\t... 24 more"
                "Caused by: java.sql.SQLException: Database failure"
                "SELECT FOO, BAR, BAZ"
                "FROM GNIP"
                "failed with ABC123"
                "\tat user$jdbc_update.invoke(user.clj:7)"
                "\tat user$make_jdbc_update_worker$reify__497.do_work(user.clj:18)"
                "\t... 25 more")))

  (is (= [{:class-name "com.datastax.driver.core.TransportException"
           :message "/17.76.3.14:9042 Cannot connect"}
          {:class-name "java.net.ConnectException"
           :message "Connection refused: /17.76.3.14:9042"
           :stack-trace [{:class "sun.nio.ch.SocketChannelImpl"
                          :file ""
                          :id "sun.nio.ch.SocketChannelImpl.checkConnect:-1"
                          :is-clojure? false
                          :line nil
                          :method "checkConnect"
                          :name ""
                          :names []
                          :package "sun.nio.ch"
                          :simple-class "SocketChannelImpl"}
                         {:class "sun.nio.ch.SocketChannelImpl"
                          :file "SocketChannelImpl.java"
                          :id "sun.nio.ch.SocketChannelImpl.finishConnect:717"
                          :is-clojure? false
                          :line 717
                          :method "finishConnect"
                          :name ""
                          :names []
                          :package "sun.nio.ch"
                          :simple-class "SocketChannelImpl"}
                         {:class "com.datastax.shaded.netty.channel.socket.nio.NioClientBoss"
                          :file "NioClientBoss.java"
                          :id "com.datastax.shaded.netty.channel.socket.nio.NioClientBoss.connect:150"
                          :is-clojure? false
                          :line 150
                          :method "connect"
                          :name ""
                          :names []
                          :package "com.datastax.shaded.netty.channel.socket.nio"
                          :simple-class "NioClientBoss"}
                         {:class "com.datastax.shaded.netty.channel.socket.nio.NioClientBoss"
                          :file "NioClientBoss.java"
                          :id "com.datastax.shaded.netty.channel.socket.nio.NioClientBoss.processSelectedKeys:105"
                          :is-clojure? false
                          :line 105
                          :method "processSelectedKeys"
                          :name ""
                          :names []
                          :package "com.datastax.shaded.netty.channel.socket.nio"
                          :simple-class "NioClientBoss"}
                         {:class "com.datastax.shaded.netty.channel.socket.nio.NioClientBoss"
                          :file "NioClientBoss.java"
                          :id "com.datastax.shaded.netty.channel.socket.nio.NioClientBoss.process:79"
                          :is-clojure? false
                          :line 79
                          :method "process"
                          :name ""
                          :names []
                          :package "com.datastax.shaded.netty.channel.socket.nio"
                          :simple-class "NioClientBoss"}
                         {:class "com.datastax.shaded.netty.channel.socket.nio.AbstractNioSelector"
                          :file "AbstractNioSelector.java"
                          :id "com.datastax.shaded.netty.channel.socket.nio.AbstractNioSelector.run:318"
                          :is-clojure? false
                          :line 318
                          :method "run"
                          :name ""
                          :names []
                          :package "com.datastax.shaded.netty.channel.socket.nio"
                          :simple-class "AbstractNioSelector"}
                         {:class "com.datastax.shaded.netty.channel.socket.nio.NioClientBoss"
                          :file "NioClientBoss.java"
                          :id "com.datastax.shaded.netty.channel.socket.nio.NioClientBoss.run:42"
                          :is-clojure? false
                          :line 42
                          :method "run"
                          :name ""
                          :names []
                          :package "com.datastax.shaded.netty.channel.socket.nio"
                          :simple-class "NioClientBoss"}
                         {:class "com.datastax.shaded.netty.util.ThreadRenamingRunnable"
                          :file "ThreadRenamingRunnable.java"
                          :id "com.datastax.shaded.netty.util.ThreadRenamingRunnable.run:108"
                          :is-clojure? false
                          :line 108
                          :method "run"
                          :name ""
                          :names []
                          :package "com.datastax.shaded.netty.util"
                          :simple-class "ThreadRenamingRunnable"}
                         {:class "com.datastax.shaded.netty.util.internal.DeadLockProofWorker$1"
                          :file "DeadLockProofWorker.java"
                          :id "com.datastax.shaded.netty.util.internal.DeadLockProofWorker$1.run:42"
                          :is-clojure? false
                          :line 42
                          :method "run"
                          :name ""
                          :names []
                          :package "com.datastax.shaded.netty.util.internal"
                          :simple-class "DeadLockProofWorker$1"}
                         {:class "com.datastax.driver.core.Connection"
                          :file "Connection.java"
                          :id "com.datastax.driver.core.Connection.<init>:104"
                          :is-clojure? false
                          :line 104
                          :method "<init>"
                          :name ""
                          :names []
                          :package "com.datastax.driver.core"
                          :simple-class "Connection"}
                         {:class "com.datastax.driver.core.PooledConnection"
                          :file "PooledConnection.java"
                          :id "com.datastax.driver.core.PooledConnection.<init>:32"
                          :is-clojure? false
                          :line 32
                          :method "<init>"
                          :name ""
                          :names []
                          :package "com.datastax.driver.core"
                          :simple-class "PooledConnection"}
                         {:class "com.datastax.driver.core.Connection$Factory"
                          :file "Connection.java"
                          :id "com.datastax.driver.core.Connection$Factory.open:557"
                          :is-clojure? false
                          :line 557
                          :method "open"
                          :name ""
                          :names []
                          :package "com.datastax.driver.core"
                          :simple-class "Connection$Factory"}
                         {:class "com.datastax.driver.core.DynamicConnectionPool"
                          :file "DynamicConnectionPool.java"
                          :id "com.datastax.driver.core.DynamicConnectionPool.<init>:74"
                          :is-clojure? false
                          :line 74
                          :method "<init>"
                          :name ""
                          :names []
                          :package "com.datastax.driver.core"
                          :simple-class "DynamicConnectionPool"}
                         {:class "com.datastax.driver.core.HostConnectionPool"
                          :file "HostConnectionPool.java"
                          :id "com.datastax.driver.core.HostConnectionPool.newInstance:33"
                          :is-clojure? false
                          :line 33
                          :method "newInstance"
                          :name ""
                          :names []
                          :package "com.datastax.driver.core"
                          :simple-class "HostConnectionPool"}
                         {:class "com.datastax.driver.core.SessionManager$2"
                          :file "SessionManager.java"
                          :id "com.datastax.driver.core.SessionManager$2.call:231"
                          :is-clojure? false
                          :line 231
                          :method "call"
                          :name ""
                          :names []
                          :package "com.datastax.driver.core"
                          :simple-class "SessionManager$2"}
                         {:class "com.datastax.driver.core.SessionManager$2"
                          :file "SessionManager.java"
                          :id "com.datastax.driver.core.SessionManager$2.call:224"
                          :is-clojure? false
                          :line 224
                          :method "call"
                          :name ""
                          :names []
                          :package "com.datastax.driver.core"
                          :simple-class "SessionManager$2"}
                         {:class "java.util.concurrent.FutureTask"
                          :file "FutureTask.java"
                          :id "java.util.concurrent.FutureTask.run:266"
                          :is-clojure? false
                          :line 266
                          :method "run"
                          :name ""
                          :names []
                          :package "java.util.concurrent"
                          :simple-class "FutureTask"}
                         {:class "java.util.concurrent.ThreadPoolExecutor"
                          :file "ThreadPoolExecutor.java"
                          :id "java.util.concurrent.ThreadPoolExecutor.runWorker:1142"
                          :is-clojure? false
                          :line 1142
                          :method "runWorker"
                          :name ""
                          :names []
                          :package "java.util.concurrent"
                          :simple-class "ThreadPoolExecutor"}
                         {:class "java.util.concurrent.ThreadPoolExecutor$Worker"
                          :file "ThreadPoolExecutor.java"
                          :id "java.util.concurrent.ThreadPoolExecutor$Worker.run:617"
                          :is-clojure? false
                          :line 617
                          :method "run"
                          :name ""
                          :names []
                          :package "java.util.concurrent"
                          :simple-class "ThreadPoolExecutor$Worker"}
                         {:class "java.lang.Thread"
                          :file "Thread.java"
                          :id "java.lang.Thread.run:745"
                          :is-clojure? false
                          :line 745
                          :method "run"
                          :name ""
                          :names []
                          :package "java.lang"
                          :simple-class "Thread"}]}]
         (parse "com.datastax.driver.core.TransportException: /17.76.3.14:9042 Cannot connect"
                "\tat com.datastax.driver.core.Connection.<init>(Connection.java:104) ~store-service.jar:na"
                "\tat com.datastax.driver.core.PooledConnection.<init>(PooledConnection.java:32) ~store-service.jar:na"
                "\tat com.datastax.driver.core.Connection$Factory.open(Connection.java:557) ~store-service.jar:na"
                "\tat com.datastax.driver.core.DynamicConnectionPool.<init>(DynamicConnectionPool.java:74) ~store-service.jar:na"
                "\tat com.datastax.driver.core.HostConnectionPool.newInstance(HostConnectionPool.java:33) ~store-service.jar:na"
                "\tat com.datastax.driver.core.SessionManager$2.call(SessionManager.java:231) store-service.jar:na"
                "\tat com.datastax.driver.core.SessionManager$2.call(SessionManager.java:224) store-service.jar:na"
                "\tat java.util.concurrent.FutureTask.run(FutureTask.java:266) na:1.8.0_66"
                "\tat java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142) na:1.8.0_66"
                "\tat java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617) na:1.8.0_66"
                "\tat java.lang.Thread.run(Thread.java:745) na:1.8.0_66"
                "Caused by: java.net.ConnectException: Connection refused: /17.76.3.14:9042"
                "\tat sun.nio.ch.SocketChannelImpl.checkConnect(Native Method) ~na:1.8.0_66"
                "\tat sun.nio.ch.SocketChannelImpl.finishConnect(SocketChannelImpl.java:717) ~na:1.8.0_66"
                "\tat com.datastax.shaded.netty.channel.socket.nio.NioClientBoss.connect(NioClientBoss.java:150) ~store-service.jar:na"
                "\tat com.datastax.shaded.netty.channel.socket.nio.NioClientBoss.processSelectedKeys(NioClientBoss.java:105) ~store-service.jar:na"
                "\tat com.datastax.shaded.netty.channel.socket.nio.NioClientBoss.process(NioClientBoss.java:79) ~store-service.jar:na"
                "\tat com.datastax.shaded.netty.channel.socket.nio.AbstractNioSelector.run(AbstractNioSelector.java:318) ~store-service.jar:na"
                "\tat com.datastax.shaded.netty.channel.socket.nio.NioClientBoss.run(NioClientBoss.java:42) ~store-service.jar:na"
                "\tat com.datastax.shaded.netty.util.ThreadRenamingRunnable.run(ThreadRenamingRunnable.java:108) ~store-service.jar:na"
                "\tat com.datastax.shaded.netty.util.internal.DeadLockProofWorker$1.run(DeadLockProofWorker.java:42) ~store-service.jar:na"
                "\t... 3 common frames omitted"))))

(deftest write-exceptions-with-nil-data
  (testing "Does not fail with a nil ex-info map key"
    (is (re-find #"nil.*nil"
                 (format-exception (ex-info "Error" {nil nil}))))))

(deftest format-stack-trace-element
  (let [frame-names (->> (Thread/currentThread)
                        .getStackTrace
                         seq
                         (mapv f/format-stack-trace-element))]
   (is (match?
         ;; A few sample Java and Clojure frame names
         (m/embeds #{"java.lang.Thread.getStackTrace"
                     "clojure.core/apply"
                     "clojure.test/run-tests"})
         (set frame-names)))))

(defn parse-and-format
  [file]
  (binding [*color-enabled* false]
    (-> file
        io/resource
        slurp
        (parse-exception nil)
        (f/format-exception* nil)
        string/split-lines)))

(deftest parse-no-message-exception
  (is (match? ["user/x  REPL Input  ┐ (repeats 255 times)"
               "user/y  REPL Input  ┘"
               "user/x  REPL Input"
               "   ..."
               "java.lang.StackOverflowError:"]
              (parse-and-format "overflow-exception.txt"))))

(deftest parse-nested-exception

  (is (match? ["                                                                      ..."
               "                                                      clojure.core/map/fn                       core.clj:2772"
               "                                                                      ..."
               "                                                   clojure.test/test-vars                       test.clj: 731"
               "                                                                      ..."
               "                                                clojure.test/test-vars/fn                       test.clj: 735"
               "                                                                      ..."
               "                                            clojure.test/test-vars/fn{x2}                       test.clj: 735"
               "                                                    clojure.test/test-var                       test.clj: 717"
               "                                                 clojure.test/test-var/fn                       test.clj: 717"
               "                                 integration.pull-request-closed-tests/fn  pull_request_closed_tests.clj:  73"
               "                                                     state-flow.core/run*                       core.clj: 359"
               "                        state-flow.helpers.runners/run-with-fn-validation                    runners.clj:   8"
               "               state-flow.helpers.runners/run-with-fn-validation/fn121504                    runners.clj:   8"
               "            state-flow.helpers.runners/run-with-fn-validation/fn121504/fn                    runners.clj:   9"
               "                                                                      ..."
               "                                                      state-flow.core/run                       core.clj: 324"
               "                                                     cats.monad.state/run                     state.cljc: 147"
               "                                 state-flow.state/error-catching-state/fn                      state.clj:  20 ┐ (repeats 2 times)"
               "                                             cats.monad.exception/wrap/fn                 exception.cljc: 250 │"
               "                                         cats.monad.exception/exec-try-on                 exception.cljc: 197 │"
               "                                cats.monad.exception/wrap/fn/func--auto--                 exception.cljc: 250 │"
               "                                                state-flow.state/reify/fn                      state.clj:  50 ┘"
               "                                 state-flow.state/error-catching-state/fn                      state.clj:  20"
               "                                             cats.monad.exception/wrap/fn                 exception.cljc: 250"
               "                                         cats.monad.exception/exec-try-on                 exception.cljc: 197"
               "                                cats.monad.exception/wrap/fn/func--auto--                 exception.cljc: 250"
               "                                                state-flow.state/reify/fn                      state.clj:  47"
               "                                 state-flow.state/error-catching-state/fn                      state.clj:  20 ┐ (repeats 6 times)"
               "                                             cats.monad.exception/wrap/fn                 exception.cljc: 250 │"
               "                                         cats.monad.exception/exec-try-on                 exception.cljc: 197 │"
               "                                cats.monad.exception/wrap/fn/func--auto--                 exception.cljc: 250 │"
               "                                                state-flow.state/reify/fn                      state.clj:  50 ┘"
               "                                                        cats.core/bind/fn                      core.cljc:  97"
               "                                                  clojure.core/partial/fn                       core.clj:2641"
               "                           integration.pull-request-closed-tests/fn/-{x3}  pull_request_closed_tests.clj:  78"
               "                        integration.pull-request-closed-tests/fn/-{x3}/fn  pull_request_closed_tests.clj:  78 ─ (repeats 2 times)"
               "                        state-flow.helpers.runners/run-with-fn-validation                    runners.clj:   8"
               "               state-flow.helpers.runners/run-with-fn-validation/fn121504                    runners.clj:   8"
               "            state-flow.helpers.runners/run-with-fn-validation/fn121504/fn                    runners.clj:   9"
               "                                                                      ..."
               "                                                      state-flow.core/run                       core.clj: 324"
               "                                                     cats.monad.state/run                     state.cljc: 147"
               "                                 state-flow.state/error-catching-state/fn                      state.clj:  20 ┐ (repeats 2 times)"
               "                                             cats.monad.exception/wrap/fn                 exception.cljc: 250 │"
               "                                         cats.monad.exception/exec-try-on                 exception.cljc: 197 │"
               "                                cats.monad.exception/wrap/fn/func--auto--                 exception.cljc: 250 │"
               "                                                state-flow.state/reify/fn                      state.clj:  50 ┘"
               "                                 state-flow.state/error-catching-state/fn                      state.clj:  20 ┐ (repeats 2 times)"
               "                                             cats.monad.exception/wrap/fn                 exception.cljc: 250 │"
               "                                         cats.monad.exception/exec-try-on                 exception.cljc: 197 │"
               "                                cats.monad.exception/wrap/fn/func--auto--                 exception.cljc: 250 │"
               "                                                state-flow.state/reify/fn                      state.clj:  47 │"
               "                                 state-flow.state/error-catching-state/fn                      state.clj:  20 │"
               "                                             cats.monad.exception/wrap/fn                 exception.cljc: 250 │"
               "                                         cats.monad.exception/exec-try-on                 exception.cljc: 197 │"
               "                                cats.monad.exception/wrap/fn/func--auto--                 exception.cljc: 250 │"
               "                                                state-flow.state/reify/fn                      state.clj:  50 ┘"
               "                                                        cats.core/bind/fn                      core.cljc:  97"
               "                                  state-flow.helpers.core/with-resource/-                       core.clj:  48"
               "                    state-flow.helpers.component.kafka/consume-message/fn                      kafka.clj:  55"
               "                           common-kafka.components.mock-consumer/consume!              mock_consumer.clj: 231"
               "      common-kafka.components.mock-consumer/eval68623/consume-raw-message              mock_consumer.clj: 155"
               "   common-kafka.components.mock-consumer/eval68623/consume-raw-message/fn              mock_consumer.clj: 158"
               "     common-kafka.components.mock-consumer/eval68591/consume-raw-message*              mock_consumer.clj: 143"
               "  common-kafka.components.mock-consumer/eval68591/consume-raw-message*/fn              mock_consumer.clj: 148"
               "                      common-kafka.internals.interceptors/eval48533/enter               interceptors.clj:  28"
               "                   common-kafka.internals.interceptors/eval48533/enter/fn               interceptors.clj:  32"
               "                    common-kafka.internals.interceptors/eval48507/execute               interceptors.clj:  15"
               "                 common-kafka.internals.interceptors/eval48507/execute/fn               interceptors.clj:  25"
               "                                    io.pedestal.interceptor.chain/execute                      chain.clj: 379"
               "                                  io.pedestal.interceptor.chain/enter-all                      chain.clj: 235"
               "                                io.pedestal.interceptor.chain/process-all                      chain.clj: 186"
               "                             io.pedestal.interceptor.chain/process-all/fn                      chain.clj: 188"
               "                   io.pedestal.interceptor.chain/process-all-with-binding                      chain.clj: 171"
               "                                      io.pedestal.interceptor.chain/try-f                      chain.clj:  54"
               "                          common-kafka.interceptors.handler/handler-in/fn                    handler.clj:  42"
               "                          common-core.fault-tolerance.attempt/attempt-map                    attempt.clj:  60"
               "                                                      clojure.core/reduce                       core.clj:6964"
               "                                                                      ..."
               "                       common-core.fault-tolerance.attempt/attempt-map/fn                    attempt.clj:  63"
               "                                                  clojure.core/partial/fn                       core.clj:2641"
               "                             common-kafka.interceptors.handler/handle-one                    handler.clj:  23"
               "            common-core.protocols.circuit-breaker/with-circuit-breaker/fn            circuit_breaker.clj:  53"
               "   common-core.fault-tolerance.circuit-breaker.ChannelCircuitBreaker/run*            circuit_breaker.clj: 269"
               "               common-core.fault-tolerance.circuit-breaker/eval43588/run*            circuit_breaker.clj: 148"
               "            common-core.fault-tolerance.circuit-breaker/eval43588/run*/fn            circuit_breaker.clj: 150"
               "               common-core.fault-tolerance.circuit-breaker/eval43560/run1            circuit_breaker.clj: 134"
               "            common-core.fault-tolerance.circuit-breaker/eval43560/run1/fn            circuit_breaker.clj: 146"
               "   common-core.fault-tolerance.circuit-breaker/eval43560/run1/fn/attempt!            circuit_breaker.clj: 139"
               "   common-core.fault-tolerance.circuit-breaker/eval43414/evaluate-command            circuit_breaker.clj:  65"
               "common-core.fault-tolerance.circuit-breaker/eval43414/evaluate-command/fn            circuit_breaker.clj:  68"
               "        common-core.protocols.circuit-breaker/with-circuit-breaker/fn{x2}            circuit_breaker.clj:  53"
               "                     common-kafka.components.consumer/adapt-handler-fn/fn                   consumer.clj: 127"
               "                       balatro.diplomat.consumer/eval104644/pull-request!                   consumer.clj:  20"
               "                    balatro.diplomat.consumer/eval104644/pull-request!/fn                   consumer.clj:  22"
               "        balatro.controllers.github-event/eval104167/pull-request-handler!               github_event.clj:   7"
               "     balatro.controllers.github-event/eval104167/pull-request-handler!/fn               github_event.clj:  16"
               "      balatro.diplomat.producer/eval98024/pr-checks-completed-transition!                   producer.clj: 139"
               "   balatro.diplomat.producer/eval98024/pr-checks-completed-transition!/fn                   producer.clj: 145"
               "                  common-kafka.components.mock-producer.Producer/produce!              mock_producer.clj: 100"
               "                 common-kafka.components.mock-producer/eval68275/produce!              mock_producer.clj:  57"
               "              common-kafka.components.mock-producer/eval68275/produce!/fn              mock_producer.clj:  60"
               "                      common-kafka.internals.interceptors/eval48559/leave               interceptors.clj:  35"
               "                   common-kafka.internals.interceptors/eval48559/leave/fn               interceptors.clj:  39"
               "                    common-kafka.internals.interceptors/eval48507/execute               interceptors.clj:  15"
               "                 common-kafka.internals.interceptors/eval48507/execute/fn               interceptors.clj:  25"
               "                                    io.pedestal.interceptor.chain/execute                      chain.clj: 379"
               "                                  io.pedestal.interceptor.chain/leave-all                      chain.clj: 266"
               "                               io.pedestal.interceptor.chain/leave-all/fn                      chain.clj: 268"
               "                     io.pedestal.interceptor.chain/leave-all-with-binding                      chain.clj: 254"
               "                                      io.pedestal.interceptor.chain/try-f                      chain.clj:  54"
               "                   common-kafka.interceptors.mock-handler/producer-out/fn               mock_handler.clj:  64"
               "        common-kafka.interceptors.mock-handler/eval67883/add-to-produced!               mock_handler.clj:  50"
               "     common-kafka.interceptors.mock-handler/eval67883/add-to-produced!/fn               mock_handler.clj:  52"
               "            common-kafka.interceptors.mock-handler/eval67822/add-to-atom!               mock_handler.clj:  40"
               "         common-kafka.interceptors.mock-handler/eval67822/add-to-atom!/fn               mock_handler.clj:  44"
               "                                                       clojure.core/swap!                       core.clj:2371"
               "                                                                      ..."
               "       common-kafka.components.mock-consumer/messages-loopback-watcher/fn              mock_consumer.clj: 297"
               "   common-kafka.components.mock-consumer/consume-newly-produced-messages!              mock_consumer.clj: 289"
               " common-kafka.components.mock-consumer/messages-loopback-watcher-consume!              mock_consumer.clj: 249"
               "                           common-kafka.components.mock-consumer/consume!              mock_consumer.clj: 231"
               "      common-kafka.components.mock-consumer/eval68623/consume-raw-message              mock_consumer.clj: 155"
               "   common-kafka.components.mock-consumer/eval68623/consume-raw-message/fn              mock_consumer.clj: 158"
               "     common-kafka.components.mock-consumer/eval68591/consume-raw-message*              mock_consumer.clj: 143"
               "  common-kafka.components.mock-consumer/eval68591/consume-raw-message*/fn              mock_consumer.clj: 152"
               "                common-kafka.interceptors.mock-errors/error-handling-item                mock_errors.clj:  50"
               "  common-kafka.components.mock-consumer/eval68591/consume-raw-message*/fn              mock_consumer.clj: 148"
               "                      common-kafka.internals.interceptors/eval48533/enter               interceptors.clj:  28"
               "                   common-kafka.internals.interceptors/eval48533/enter/fn               interceptors.clj:  32"
               "                    common-kafka.internals.interceptors/eval48507/execute               interceptors.clj:  15"
               "                 common-kafka.internals.interceptors/eval48507/execute/fn               interceptors.clj:  25"
               "                                    io.pedestal.interceptor.chain/execute                      chain.clj: 379"
               "                                  io.pedestal.interceptor.chain/enter-all                      chain.clj: 235"
               "                                io.pedestal.interceptor.chain/process-all                      chain.clj: 186"
               "                             io.pedestal.interceptor.chain/process-all/fn                      chain.clj: 188"
               "                   io.pedestal.interceptor.chain/process-all-with-binding                      chain.clj: 171"
               "                                      io.pedestal.interceptor.chain/try-f                      chain.clj:  54"
               "                          common-kafka.interceptors.handler/handler-in/fn                    handler.clj:  42"
               "                          common-core.fault-tolerance.attempt/attempt-map                    attempt.clj:  60"
               "                                                      clojure.core/reduce                       core.clj:6964"
               "                                                                      ..."
               "                       common-core.fault-tolerance.attempt/attempt-map/fn                    attempt.clj:  63"
               "                                                  clojure.core/partial/fn                       core.clj:2641"
               "                             common-kafka.interceptors.handler/handle-one                    handler.clj:  23"
               "            common-core.protocols.circuit-breaker/with-circuit-breaker/fn            circuit_breaker.clj:  53"
               "   common-core.fault-tolerance.circuit-breaker.ChannelCircuitBreaker/run*            circuit_breaker.clj: 269"
               "               common-core.fault-tolerance.circuit-breaker/eval43588/run*            circuit_breaker.clj: 148"
               "            common-core.fault-tolerance.circuit-breaker/eval43588/run*/fn            circuit_breaker.clj: 150"
               "               common-core.fault-tolerance.circuit-breaker/eval43560/run1            circuit_breaker.clj: 134"
               "            common-core.fault-tolerance.circuit-breaker/eval43560/run1/fn            circuit_breaker.clj: 146"
               "   common-core.fault-tolerance.circuit-breaker/eval43560/run1/fn/attempt!            circuit_breaker.clj: 139"
               "   common-core.fault-tolerance.circuit-breaker/eval43414/evaluate-command            circuit_breaker.clj:  65"
               "common-core.fault-tolerance.circuit-breaker/eval43414/evaluate-command/fn            circuit_breaker.clj:  68"
               "        common-core.protocols.circuit-breaker/with-circuit-breaker/fn{x2}            circuit_breaker.clj:  53"
               "                     common-kafka.components.consumer/adapt-handler-fn/fn                   consumer.clj: 127"
               "                      balatro.diplomat.consumer/eval104673/transition-pr!                   consumer.clj:  44"
               "                   balatro.diplomat.consumer/eval104673/transition-pr!/fn                   consumer.clj:  48"
               "         balatro.controllers.pull-request/eval104468/transition-pr-by-id!               pull_request.clj: 184"
               "      balatro.controllers.pull-request/eval104468/transition-pr-by-id!/fn               pull_request.clj: 190"
               "               balatro.controllers.pull-request/eval104404/transition-pr!               pull_request.clj: 158"
               "            balatro.controllers.pull-request/eval104404/transition-pr!/fn               pull_request.clj: 163"
               "                                                                      ..."
               "                           balatro.controllers.pull-request/eval104365/fn               pull_request.clj: 137"
               "         balatro.controllers.pull-request/eval104270/pr-checks-completed!               pull_request.clj:  46"
               "      balatro.controllers.pull-request/eval104270/pr-checks-completed!/fn               pull_request.clj:  56"
               "                       balatro.diplomat.github/eval96773/pr-checks-status                     github.clj:  48"
               "                    balatro.diplomat.github/eval96773/pr-checks-status/fn                     github.clj:  53"
               "                       balatro.components.github-api/eval96353/pr-checks!                 github_api.clj: 224"
               "                    balatro.components.github-api/eval96353/pr-checks!/fn                 github_api.clj: 228"
               "                          balatro.components.github-api/eval96129/get-pr!                 github_api.clj:  83"
               "                       balatro.components.github-api/eval96129/get-pr!/fn                 github_api.clj:  87"
               "                    common-github.components.github-api.impl/request-loop                       impl.clj: 131"
               "                   balatro.components.github-api/eval96129/get-pr!/fn{x2}                 github_api.clj:  87"
               "              common-github.components.github-api.impl/unhandled-response                       impl.clj:  86"
               "clojure.lang.ExceptionInfo: GitHub API request to /repos/nubank/giraffe/pulls/123 returned status 2000"
               "clojure.lang.ExceptionInfo: Error handling item"]
              (parse-and-format "nested-exception.txt"))))
