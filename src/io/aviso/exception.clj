(ns io.aviso.exception
  "Format and present exceptions in a pretty (structured, formatted) way."
  (:use io.aviso.ansi)
  (:require [clojure
             [pprint :as pp]
             [set :as set]
             [string :as str]]
            [io.aviso
             [columns :as c]
             [writer :as w]])
  (:import [java.lang StringBuilder StackTraceElement]
           [clojure.lang Compiler MultiFn ExceptionInfo]
           [java.lang.reflect Field]
           [java.util.regex Pattern]))


(def ^{:dynamic true
       :added   "0.1.15"}
*traditional*
  "If bound to true, then exceptions will be formatted the traditional way (the only option prior to 0.1.15)."
  false)

(defn- length [^String s] (.length s))

(defn- strip-prefix
  [^String prefix ^String input]
  (let [prefix-len (.length prefix)]
    (if (and (.startsWith input prefix)
             (< prefix-len (.length input)))
      (.substring input prefix-len)
      input)))

(def ^:private current-dir-prefix
  "Convert the current directory (via property 'user.dir') into a prefix to be omitted from file names."
  (delay (str (System/getProperty "user.dir") "/")))

(defn- ?reverse
  [reverse? coll]
  (if reverse?
    (reverse coll)
    coll))

;;; Obviously, this is making use of some internals of Clojure that
;;; could change at any time.

(def ^:private clojure->java
  (->> (Compiler/CHAR_MAP)
       set/map-invert
       (sort-by #(-> % first length))
       reverse))


(defn- match-mangled
  [^String s i]
  (->> clojure->java
       (filter (fn [[k _]] (.regionMatches s i k 0 (length k))))
       ;; Return the matching sequence and its single character replacement
       first))

(defn demangle
  "De-mangle a Java name back to a Clojure name by converting mangled sequences, such as \"_QMARK_\"
  back into simple characters."
  [^String s]
  (let [in-length (.length s)
        result    (StringBuilder. in-length)]
    (loop [i 0]
      (cond
        (>= i in-length) (.toString result)
        (= \_ (.charAt s i)) (let [[match replacement] (match-mangled s i)]
                               (.append result replacement)
                               (recur (+ i (length match))))
        :else (do
                (.append result (.charAt s i))
                (recur (inc i)))))))

(defn- match-keys
  "Apply the function f to all values in the map; where the result is truthy, add the key to the result."
  [m f]
  ;; (seq m) is necessary because the source is via (bean), which returns an odd implementation of map
  (reduce (fn [result [k v]] (if (f v) (conj result k) result)) [] (seq m)))

(defn- wrap-exception
  [^Throwable exception properties next-exception]
  [{:exception  exception
    :class-name (-> exception .getClass .getName)
    :message    (.getMessage exception)
    :properties properties}
   next-exception])

(defn- expand-exception
  [^Throwable exception]
  (if (instance? ExceptionInfo exception)
    (wrap-exception exception (.getData ^ExceptionInfo exception) (.getCause exception))
    (let [properties              (bean exception)
          nil-property-keys       (match-keys properties nil?)
          throwable-property-keys (match-keys properties #(.isInstance Throwable %))
          remove'                 #(remove %2 %1)
          nested-exception        (-> properties
                                      (select-keys throwable-property-keys)
                                      vals
                                      (remove' nil?)
                                      ;; Avoid infinite loop!
                                      (remove' #(= % exception))
                                      first)
          ;; Ignore basic properties of Throwable, any nil properties, and any properties
          ;; that are themselves Throwables
          discarded-keys          (concat [:suppressed :message :localizedMessage :class :stackTrace]
                                          nil-property-keys
                                          throwable-property-keys)
          retained-properties     (apply dissoc properties discarded-keys)]
      (wrap-exception exception retained-properties nested-exception))))


(def ^{:added   "0.1.18"
       :dynamic true}
*default-frame-rules*
  "The set of rules that forms the basis for [[*default-frame-filter*]], as a vector or vectors.

  Each rule is three values:

  * A function that extracts the value from the stack frame map (typically, this is a keyword such
  as :package or :name). The value is converted to a string.

  * A string or regexp used for matching.
  
  * A resulting frame visibility (:hide, :omit, :terminate, or :show).

  The default rules:

  * omit everything in clojure.lang and java.lang.reflect.
  * hide everything in sun.reflect
  * terminate at speclj.* or clojure.main/repl/read-eval-print
  "
  [[:package "clojure.lang" :omit]
   [:package #"sun\.reflect.*" :hide]
   [:package "java.lang.reflect" :omit]
   [:name #"speclj\..*" :terminate]
   [:name #"clojure\.main/repl/read-eval-print.*" :terminate]])

(defn- apply-rule
  [frame [f match visibility :as rule]]
  (let [value (str (f frame))]
    (cond
      (string? match)
      (if (= match value) visibility)

      (instance? Pattern match)
      (if (re-matches match value) visibility)

      :else
      (throw (ex-info "Unexpected match type in rule."
                      {:rule rule})))))

(defn *default-frame-filter*
  "Default stack frame filter used when printing REPL exceptions. This will omit frames in the `clojure.lang`
  and `java.lang.reflect` package, hide frames in the `sun.reflect` package,
  and terminates the stack trace at the read-eval-print loop frame."
  {:added   "0.1.16"
   :dynamic true}
  [frame]
  (-> (keep #(apply-rule frame %) *default-frame-rules*)
      first
      (or :show)))

(defn analyze-exception
  "Converts an exception into a seq of maps representing nested exceptions. Each map
  contains:

  :exception Throwable
  : the original Throwable instance

  :class-name String
  : name of the Java class for the exception

  :message String
  : value of the exception's message property (possibly nil)

  :properties Map
  : map of properties to present

  The :properties map does not include any properties that are assignable to type Throwable.

  The first property that is assignable to type Throwable (not necessarily the rootCause property)
  will be used as the nested exception (for the next map in the sequence)."
  [^Throwable e]
  (loop [result  []
         current e]
    (let [[expanded nested] (expand-exception current)]
      (if nested
        (recur (conj result expanded) nested)
        (conj result expanded)))))

(defn- update-keys [m f]
  "Builds a map where f has been applied to each key in m."
  (into {} (map (fn [[k v]] [(f k) v]) m)))

(defn- convert-to-clojure
  [class-name method-name]
  (let [[namespace-name & raw-function-ids] (str/split class-name #"\$")
        ;; Clojure adds __1234 unique ids to the ends of things, remove those.
        function-ids (map #(str/replace % #"__\d+" "") raw-function-ids)
        ;; In a degenerate case, a protocol method could be called "invoke" or "doInvoke"; we're ignoring
        ;; that possibility here and assuming it's the IFn.invoke() or doInvoke().
        all-ids      (if (#{"invoke" "doInvoke"} method-name)
                       function-ids
                       (-> function-ids vec (conj method-name)))]
    ;; The assumption is that no real namespace or function name will contain underscores (the underscores
    ;; are name-mangled dashes).
    (->>
      (cons namespace-name all-ids)
      (map demangle))))

(defn- expand-stack-trace-element
  [file-name-prefix ^StackTraceElement element]
  (let [class-name  (.getClassName element)
        method-name (.getMethodName element)
        dotx        (.lastIndexOf class-name ".")
        file-name   (or (.getFileName element) "")
        is-clojure? (.endsWith file-name ".clj")
        names       (if is-clojure? (convert-to-clojure class-name method-name) [])
        name        (str/join "/" names)
        line        (-> element .getLineNumber)]
    {:file         (strip-prefix file-name-prefix file-name)
     :line         (if (pos? line) line)
     :class        class-name
     :package      (if (pos? dotx) (.substring class-name 0 dotx))
     :simple-class (if (pos? dotx)
                     (.substring class-name (inc dotx))
                     class-name)
     :method       method-name
     ;; Used to calculate column width
     :name         name
     ;; Used to present compound Clojure name with last term highlighted
     :names        names}))

(def ^:private empty-stack-trace-warning
  "Stack trace of root exception is empty; this is likely due to a JVM optimization that can be disabled with -XX:-OmitStackTraceInFastThrow.")

(defn expand-stack-trace
  "Extracts the stack trace for an exception and returns a seq of expanded stack frame maps:

  :file String
  : file name

  :line Integer
  : line number as an integer, or nil

  :class String
  : complete Java class name

  :package String
  : Java package name, or nil for root package

  :simple-class String
  : simple name of Java class (without package prefix)

  :method String
  : Java method name

  :name String
  : Fully qualified Clojure name (demangled from the Java class name), or the empty string for non-Clojure stack frames

  :names seq of String
  : Clojure name split at slashes (empty for non-Clojure stack frames)"
  [^Throwable exception]
  (let [elements (map (partial expand-stack-trace-element @current-dir-prefix) (.getStackTrace exception))]
    (when (empty? elements)
      (binding [*out* *err*]
        (println empty-stack-trace-warning)
        (flush)))
    elements))

(def ^:dynamic *fonts*
  "ANSI fonts for different elements in the formatted exception report."
  {:exception     bold-red-font
   :reset         reset-font
   :message       italic-font
   :property      bold-font
   :source        green-font
   :function-name bold-yellow-font
   :clojure-frame yellow-font
   :java-frame    white-font
   :omitted-frame white-font})

(defn- preformat-stack-frame
  [frame]
  (cond
    (:omitted frame)
    (assoc frame :formatted-name (str (:omitted-frame *fonts*) "..." (:reset *fonts*))
                 :file ""
                 :line nil)

    ;; When :names is empty, it's a Java (not Clojure) frame
    (-> frame :names empty?)
    (let [full-name      (str (:class frame) "." (:method frame))
          formatted-name (str (:java-frame *fonts*) full-name (:reset *fonts*))]
      (assoc frame
        :formatted-name formatted-name))

    :else
    (let [names          (:names frame)
          formatted-name (str
                           (:clojure-frame *fonts*)
                           (->> names drop-last (str/join "/"))
                           "/"
                           (:function-name *fonts*) (last names) (:reset *fonts*))]
      (assoc frame :formatted-name formatted-name))))

(defn- apply-frame-filter
  [frame-filter frames]
  (if (nil? frame-filter)
    frames
    (loop [result   []
           [frame & more-frames] frames
           omitting false]
      (case (if frame (frame-filter frame) :terminate)

        :terminate
        result

        :show
        (recur (conj result frame)
               more-frames
               false)

        :hide
        (recur result more-frames omitting)

        :omit
        (if omitting
          (recur result more-frames true)
          (recur (conj result (assoc frame :omitted true))
                 more-frames
                 true))))))

(defn- is-repeat?
  [left-frame right-frame]
  (and (= (:formatted-name left-frame)
          (:formatted-name right-frame))
       (= (:line left-frame)
          (:line right-frame))))

(defn- repeating-frame-reducer
  [output-frames frame]
  (let [output-count      (count output-frames)
        last-output-index (dec output-count)]
    (cond
      (zero? output-count)
      (conj output-frames frame)

      (is-repeat? (output-frames last-output-index) frame)
      (update-in output-frames [last-output-index :repeats]
                 (fnil inc 1))

      :else
      (conj output-frames frame))))


(defn- format-repeats
  [{:keys [repeats]}]
  (if repeats
    (format " (repeats %,d times)" repeats)))

(defn- write-stack-trace
  [writer exception frame-limit frame-filter modern?]
  (let [elements  (->> exception
                       expand-stack-trace
                       (apply-frame-filter frame-filter)
                       (map preformat-stack-frame)
                       (reduce repeating-frame-reducer []))
        elements' (if frame-limit (take frame-limit elements) elements)]
    (c/write-rows writer [:formatted-name
                          "  "
                          (:source *fonts*)
                          :file
                          [#(if (:line %) ": ") :left 2]
                          #(-> % :line str)
                          [format-repeats :none]
                          (:reset *fonts*)]
                  (?reverse modern? elements'))))

(defmulti exception-dispatch
          "The pretty print dispatch function used when formatting exception output (specifically, when
          printing the properties of an exception). Normally, this is the same as the simple-dispatch
          (in clojure.pprint) but can be extended for specific cases:

              (import com.stuartsierra.component.SystemMap)

              (defmethod exception-dispatch SystemMap [system-map] (print \"#<SystemMap>\"))

          This ensures that the SystemMap record, wherever it appears in the exception output,
          is represented as the string `#<SystemMap>`; normally it would print as a deeply nested
          set of maps.

          This same approach can be adapted to any class or type whose structure is problematic
          for presenting in the exception output, whether for size and complexity reasons, or due to
          security concerns."
          class)

;; Not totally happy about this approach as it feels a bit hackish, and has the problem that it
;; takes a snapshot of pp/simple-dispatch *at some point*.

(let [^Field f (.getDeclaredField MultiFn "methodTable")]
  (.setAccessible f true)
  (.set f exception-dispatch (methods pp/simple-dispatch)))

(defn- format-property-value
  [value]
  (pp/write value :stream nil :length (or *print-length* 10) :dispatch exception-dispatch))



(defn write-exception
  "Writes a formatted version of the exception to the [[StringWriter]]. By default, writes to *out* and includes
  the stack trace, with no frame limit.

  Output may be traditional or modern, as controlled by [[*traditional*]].
  Traditional is the typical output order for Java: the stack of exceptions comes first (outermost to
  innermost) followed by the stack trace of the innermost exception, with the frames
  in deepest to shallowest order.

  Modern output is more readable; the stack trace comes first and is reversed: shallowest frame to deepest.
  Then the exception stack is output, from the root exception to the outermost exception.
  The modern output order is more readable, as it puts the most useful information together at the bottom, so that
  it is not necessary to scroll back to see, for example, where the exception occured.

  A frame filter may be specified; the frame filter is passed each stack frame (as generated by
  [[expand-stack-trace]]).

  The filter must return one of the following values:

  :show
  : is the normal state; display the stack frame.

  :hide
  : prevents the frame from being displayed, as if it never existed.

  :omit
  : replaces the frame with a \"...\" placeholder; multiple consecutive :omits will be collapsed to a single line.
    Use :omit for \"uninteresting\" stack frames.

  :terminate
  : hides the frame AND all later frames.

  The default filter is [[*default-frame-filter*]].  An explicit filter of nil will display all stack frames.

  Repeating lines are collapsed to a single line, with a repeat count. Typically, this is the result of
  an endless loop that terminates with a StackOverflowException.

  When set, the frame limit is the number of stack frames to display; if non-nil, then some of the outermost
  stack frames may be omitted. It may be set to 0 to omit the stack trace entirely (but still display
  the exception stack).  The frame limit is applied after the frame filter (which may hide or omit frames) and
  after repeating stack frames have been identified and coallesced ... :frame-limit is really the number
  of _output_ lines to present.

  Properties of exceptions will be output using Clojure's pretty-printer, honoring all of the normal vars used
  to configure pretty-printing; however, if `*print-length*` is left as its default (nil), the print length will be set to 10.
  This is to ensure that infinite lists do not cause endless output or other exceptions.

  The `*fonts*` var contains ANSI definitions for how fonts are displayed; bind it to nil to remove ANSI formatting entirely."
  ([exception]
   (write-exception *out* exception))
  ([writer exception]
   (write-exception writer exception nil))
  ([writer exception {show-properties? :properties
                      frame-limit      :frame-limit
                      frame-filter     :filter
                      :or              {show-properties? true
                                        frame-filter     *default-frame-filter*}}]
   (let [exception-font        (:exception *fonts*)
         message-font          (:message *fonts*)
         property-font         (:property *fonts*)
         reset-font            (:reset *fonts* "")
         modern?               (not *traditional*)
         exception-stack       (->> exception
                                    analyze-exception
                                    (map #(assoc % :name (-> % :exception class .getName))))
         exception-formatter   (c/format-columns [:right (c/max-value-length exception-stack :name)]
                                                 ": "
                                                 :none)
         write-exception-stack #(doseq [e (?reverse modern? exception-stack)]
                                 (let [^Throwable exception (-> e :exception)
                                       class-name           (:name e)
                                       message              (.getMessage exception)]
                                   (exception-formatter writer
                                                        (str exception-font class-name reset-font)
                                                        (str message-font message reset-font))
                                   (when show-properties?
                                     (let [properties         (update-keys (:properties e) name)
                                           prop-keys          (keys properties)
                                           ;; Allow for the width of the exception class name, and some extra
                                           ;; indentation.
                                           property-formatter (c/format-columns "    "
                                                                                [:right (c/max-length prop-keys)]
                                                                                ": "
                                                                                :none)]
                                       (doseq [k (sort prop-keys)]
                                         (property-formatter writer
                                                             (str property-font k reset-font)
                                                             (-> properties (get k) format-property-value)))))))
         root-exception        (-> exception-stack last :exception)]

     (if *traditional*
       (write-exception-stack))
     (write-stack-trace writer root-exception frame-limit frame-filter modern?)
     (if modern?
       (write-exception-stack)))

   (w/flush-writer writer)))

(defn format-exception
  "Formats an exception as a multi-line string using [[write-exception]]."
  ([exception]
   (format-exception exception nil))
  ([exception options]
   (w/into-string write-exception exception options)))
