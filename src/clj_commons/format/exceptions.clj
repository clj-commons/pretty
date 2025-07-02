(ns clj-commons.format.exceptions
  "Format and output exceptions in a pretty (structured, formatted) way."
  (:require [clojure.pprint :as pp]
            [clojure.set :as set]
            [clojure.string :as str]
            [clj-commons.ansi :refer [compose perr]]
            [clj-commons.pretty-impl :refer [padding]])
  (:refer-clojure :exclude [*print-level* *print-length*])
  (:import (java.lang StringBuilder StackTraceElement)
           (clojure.lang Compiler ExceptionInfo Named)
           (java.util.regex Pattern)))

(def default-fonts
  "A default map of [[compose]] font defs for different elements in the formatted exception report."
  {:exception :bold.red
   :message :italic
   :property :bold
   :source :green
   :app-frame :bold.yellow
   :function-name :bold.yellow
   :clojure-frame :yellow
   :java-frame :bright-black
   :omitted-frame :faint.bright-black})

(def ^:dynamic *app-frame-names*
  "Set of strings or regular expressions defining the application's namespaces, which allows
  such namespaces to be highlighted in exception output."
  nil)

(def ^:dynamic *fonts*
  "Current set of fonts used in exception formatting. This can be overridden to change colors, or bound to nil
   to disable fonts.  Defaults are defined by [[default-fonts]]."
  default-fonts)

(def ^{:dynamic true
       :added "0.1.15"}
  *traditional*
  "If bound to true, then exceptions will be formatted the traditional way - the same as Java exceptions
  with the deepest stack frame first.  By default, the stack trace is inverted, so that the deepest
  stack frames come last, mimicking chronological order."
  false)

(defn- length
  [^String s]
  (if s
    (.length s)
    0))

(defn- strip-prefix
  [^String prefix ^String input]
  (let [prefix-len (.length prefix)]
    (if (and (str/starts-with? input prefix)
             (< prefix-len (.length input)))
      (subs input prefix-len)
      input)))

(def ^:private current-dir-prefix
  "Convert the current directory (via property 'user.dir') into a prefix to be omitted from file names."
  (str (System/getProperty "user.dir") "/"))

(defn- ?reverse
  [reverse? coll]
  (if reverse?
    (reverse coll)
    coll))

;;; Obviously, this is making use of some internals of Clojure that
;;; could change at any time.

(def ^:private clojure->java
  (->> Compiler/CHAR_MAP
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
        result (StringBuilder. in-length)]
    (loop [i 0]
      (cond
        (>= i in-length) (.toString result)
        (= \_ (.charAt s i)) (let [[match replacement] (match-mangled s i)]
                               (.append result replacement)
                               (recur (long (+ i (length match)))))
        :else (do
                (.append result (.charAt s i))
                (recur (inc i)))))))

(defn- match-keys
  "Apply the function f to all values in the map; where the result is truthy, add the key to the result."
  [m f]
  ;; (seq m) is necessary because the source is via (bean), which returns an odd implementation of map
  (reduce (fn [result [k v]] (if (f v) (conj result k) result)) [] (seq m)))

(def ^{:added "3.2.0"} default-frame-rules
  "The set of rules that forms the default for [[*default-frame-rules*]], and the
  basis for [[*default-frame-filter*]], as a vector of vectors.

 Each rule is a vector of three values:

 * A function that extracts the value from the stack frame map (typically, this is a keyword such
 as :package or :name). The value is converted to a string.
 * A string or regexp used for matching.  Strings must match exactly.
 * A resulting frame visibility (:hide, :omit, :terminate, or :show).

 The default rules:

 * omit everything in `clojure.lang`, `java.lang.reflect`
 * omit `clojure.core/with-bindings*` and `clojure.core/apply`
 * hide everything in `sun.reflect`
 * omit a number of functions in `clojure.test`
 * terminate at `speclj.*`, `clojure.main/.*`, or `nrepl.middleware.interruptible-eval`
 "
  [[:name "clojure.core/apply" :hide]
   [:id #"\Qclojure.lang.AFn.applyTo\E(Helper)?.*" :hide]
   [:package "clojure.lang" :omit]
   [:name "clojure.core/with-bindings*" :hide]
   [:package #"sun\.reflect.*" :hide]
   [:package "java.lang.reflect" :omit]
   [:name #"speclj\..*" :terminate]
   [:name #"\Qnrepl.middleware.interruptible-eval/\E.*" :terminate]
   [:name #"\Qclojure.main/\E.*" :terminate]
   [:name "clojure.test/run-tests" :terminate]
   [:name #"\Qclojure.test/\E(test-ns|test-all-vars|default-fixture).*" :omit]])

(def ^{:added "0.1.18"
       :dynamic true}
  *default-frame-rules*
  "The set of rules that forms the basis for [[*default-frame-filter*]], as a vector of vectors,
  initialized from [[default-frame-rules]]."
  default-frame-rules)

(defn- apply-rule
  [frame [f match visibility :as rule]]
  (let [value (str (f frame))]
    (cond
      (string? match)
      (when (= match value) visibility)

      (instance? Pattern match)
      (when (re-matches match value) visibility)

      :else
      (throw (ex-info "unexpected match type in rule"
                      {:rule rule})))))

(defn *default-frame-filter*
  "Default stack frame filter used when printing REPL exceptions; default value is derived from [[*default-frame-rules*]]."
  {:added "0.1.16"
   :dynamic true}
  [frame]
  (or
    (reduce (fn [_ rule]
              (when-let [result (apply-rule frame rule)]
                (reduced result)))
            nil
            *default-frame-rules*)
    :show))

(defn- convert-to-clojure
  [class-name method-name]
  (let [[namespace-name & raw-function-ids] (str/split class-name #"\$")
        ;; Clojure adds __1234 unique ids to the ends of things, remove those.
        function-ids (map #(str/replace % #"__\d+" "") raw-function-ids)
        ;; In a degenerate case, a protocol method could be called "invoke" or "doInvoke"; we're ignoring
        ;; that possibility here and assuming it's the IFn.invoke(), doInvoke() or
        ;; the invokeStatic method introduced with direct linking in Clojure 1.8.
        all-ids (if (#{"invoke" "doInvoke" "invokeStatic" "invokePrim"} method-name)
                  function-ids
                  (-> function-ids vec (conj method-name)))]
    ;; The assumption is that no real namespace or function name will contain underscores (the underscores
    ;; are name-mangled dashes).
    (->>
      (cons namespace-name all-ids)
      (mapv demangle))))

(defn- extension
  [^String file-name]
  (let [x (str/last-index-of file-name ".")]
    (when (and x (pos? x))
      (subs file-name (inc x)))))

(def ^:private clojure-extensions
  #{"clj" "cljc"})

(defn- is-repl-input?
  [file-name]
  (boolean
    (or
      (= "NO_SOURCE_FILE" file-name)
      ; This pattern comes from somewhere inside nREPL, I believe - may be dated
      (re-matches #"form-init\d+\.clj" file-name))))

(defn- transform-stack-trace-element
  [file-name-prefix *cache ^StackTraceElement element]
  (or (get @*cache element)
      (let [class-name (.getClassName element)
            method-name (.getMethodName element)
            dotx (str/last-index-of class-name ".")
            file-name (or (.getFileName element) "")
            repl-input (is-repl-input? file-name)
            [file line] (if repl-input
                          ["REPL Input"]
                          [(strip-prefix file-name-prefix file-name)
                           (-> element .getLineNumber)])
            is-clojure? (or repl-input
                            (->> file-name extension (contains? clojure-extensions)))
            names (if is-clojure? (convert-to-clojure class-name method-name) [])
            name (str/join "/" names)
            id (cond-> (if is-clojure?
                         name
                         (str class-name "." method-name))
                       line (str ":" line))
            expanded {:file file
                      :line (when (and line
                                       (pos? line))
                              line)
                      :class class-name
                      :package (when dotx (subs class-name 0 dotx))
                      :is-clojure? is-clojure?
                      :simple-class (if dotx
                                      (subs class-name (inc dotx))
                                      class-name)
                      :method method-name
                      ;; Used to detect repeating frames
                      :id id
                      ;; Used to calculate column width
                      :name name
                      ;; Used to present compound Clojure name with last term highlighted
                      :names names}]
        (vswap! *cache assoc element expanded)
        expanded)))

(defn- apply-frame-filter
  [frame-filter frames]
  (if (nil? frame-filter)
    frames
    (let [*omitting? (volatile! false)
          result (reduce (fn [result frame]
                           (case (frame-filter frame)
                             :terminate
                             (reduced result)

                             :show
                             (do
                               (vreset! *omitting? false)
                               (conj! result frame))

                             :hide
                             result

                             :omit
                             (if @*omitting?
                               result
                               (do
                                 (vreset! *omitting? true)
                                 (conj! result (assoc frame :omitted true))))))
                         (transient [])
                         frames)]
      (persistent! result))))

(defn- remove-direct-link-frames
  "With Clojure 1.8, in code (such as clojure.core) that is direct linked,
  you'll often see an invokeStatic() and/or invokePrim() frame invoked from an invoke() frame
  of the same class (the class being a compiled function). That ends up looking
  like a two-frame repeat, which is not accurate.

  This function filters out the .invoke frames so that a single Clojure
  function call is represented in the output as a single stack frame."
  [elements]
  (loop [filtered (transient [])
         prev-frame nil
         remaining elements]
    (if (empty? remaining)
      (persistent! filtered)
      (let [[this-frame & rest] remaining]
        (if (and prev-frame
                 (:is-clojure? prev-frame)
                 (:is-clojure? this-frame)
                 (= (:class prev-frame) (:class this-frame))
                 (= "invokeStatic" (:method prev-frame))
                 (contains? #{"invoke" "invokePrim"} (:method this-frame)))
          (recur filtered this-frame rest)
          (recur (conj! filtered this-frame)
                 this-frame
                 rest))))))

(defn- is-repeat?
  [left-frame right-frame]
  (= (:id left-frame)
     (:id right-frame)))

(defn- repeating-frame-reducer
  [output-frames frame]
  (let [output-count (count output-frames)
        last-output-index (dec output-count)]
    (cond
      (zero? output-count)
      (conj output-frames frame)

      (is-repeat? (output-frames last-output-index) frame)
      (update-in output-frames [last-output-index :repeats]
                 (fnil inc 1))

      :else
      (conj output-frames frame))))

(def ^:private stack-trace-warning
  (delay
    (perr
      [:bright-yellow "WARNING: "]
      "Stack trace of root exception is empty; this is likely due to a JVM optimization that can be disabled with "
      [:bold "-XX:-OmitStackTraceInFastThrow"] ".")
    (flush)))

(defn transform-stack-trace
  "Transforms a seq of StackTraceElement objects into a seq of stack frame maps:

  Key           | Type          | Description
  ---           |---            |---
  :file         | String        | Source file name, or nil if not known
  :line         | Integer       | Line number as integer, or nil
  :class        | String        | Fully qualified Java class name
  :package      | String        | Java package name, or nil for root package
  :simple-class | String        | Simple name of Java class, without the package prefix
  :method       | String        | Java method name
  :is-clojure?  | Boolean       | If true, this represents a Clojure function call, rather than a Java method invocation
  :id           | String        | An id that can be used to identify repeating stack frames; consists of the fully qualified method name (for Java frames) or fully qualified Clojure name (for Clojure frames) appended with the line number.
  :name         | String        | Fully qualified Clojure name (demangled from the Java class name), or the empty string for non-Clojure stack frames
  :names        | seq of String | Clojure name split at slashes (empty for non-Clojure stack frames)"
  {:added "3.0.0"}
  [elements]
  (let [*cache (volatile! {})]
    (map #(transform-stack-trace-element current-dir-prefix *cache %) elements)))

(defn expand-stack-trace
  "Extracts the stack trace for an exception and returns a seq of stack frame maps; a wrapper around
  [[transform-stack-trace]]."
  [^Throwable exception]
  (let [elements (.getStackTrace exception)]
    (when (empty? elements)
      @stack-trace-warning)
    (transform-stack-trace elements)))

(defn- clj-frame-font-key
  "Returns the font key to use for a Clojure stack frame.

  When provided a frame matching *app-frame-names*, returns :app-frame, otherwise :clojure-frame."
  [frame]
  (or
    (when *app-frame-names*
      (reduce (fn [_ app-frame-name]
                (when-let [match (apply-rule frame [:name app-frame-name :app-frame])]
                  (reduced match)))
              nil
              *app-frame-names*))
    :clojure-frame))

(defn- counted-terms
  [terms]
  (if-not (seq terms)
    []
    (loop [acc-term (first terms)
           acc-count 1
           ts (next terms)
           result []]
      (if (nil? ts)
        (conj result [acc-term acc-count])
        (let [t (first ts)
              ts' (next ts)]
          (if (= acc-term t)
            (recur acc-term (inc acc-count) ts' result)
            (recur t 1 ts' (conj result [acc-term acc-count]))))))))

(defn- counted-frame-name
  [[name count]]
  (if (= count 1)
    name
    (str name "{x" count "}")))

(defn- format-clojure-frame-base
  [frame]
  (let [names' (->> frame
                    :names
                    counted-terms
                    (map counted-frame-name))
        width (->> names'
                   (map length)
                   (reduce + 0)
                   (+ (count names'))                       ;; each name has a trailing slash
                   dec)]                                    ;; except the last
    {:name-width width
     :name [(get *fonts* (clj-frame-font-key frame))
            (->> names' drop-last (str/join "/"))
            "/"
            [(:function-name *fonts*) (last names')]]}))

(defn format-stack-frame
  "Transforms an expanded stack frame (see [[transform-stack-trace]])
  into a formatted stack frame:

  Key         | Type            | Description
  ---         |---              |---
  :name       | composed string | Formatted version of the stack frame :name (or :names)
  :name-width | Integer         | Visual width of the name
  :file       | String          | Location of source (or nil)
  :line       | String          | Location of source (or nil)
  :repeats    | Integer         | Number of times the frame repeats (or nil)

  Formatting is based on whether the frame is omitted, and whether it is a Clojure or Java frame."
  {:added "0.3.0"}
  [{:keys [file line names repeats] :as frame}]
  (cond
    (:omitted frame)
    {:name [(:omitted-frame *fonts*) "..."]
     :name-width 3}

    ;; When :names is empty, it's a Java (not Clojure) frame
    (empty? names)
    (let [full-name (str (:class frame) "." (:method frame))]
      {:name [(:java-frame *fonts*) full-name]
       :name-width (length full-name)
       :file file
       :line (str line)
       :repeats repeats})

    :else
    (assoc (format-clojure-frame-base frame)
      :file file
      :line (str line)
      :repeats repeats)))

(defn filter-stack-trace-maps
  "Filters the stack trace maps (from [[transform-stack-trace]], removing unnecessary frames and
  applying a filter and optional frame-limit (:filter and :frame-limit options).

  The default frame filter is [[*default-frame-filter*]].

  Returns the elements, filtered, and (in some cases) with an additional :omitted key
  (true for frames that should be omitted). This includes discarding elements that
  the filter indicates to :hide, and coalescing frames the filter indicates to :omit."
  {:added "0.3.0"}
  ([elements]
   (filter-stack-trace-maps elements nil))
  ([elements options]
   (let [frame-filter (:filter options *default-frame-filter*)
         frame-limit (:frame-limit options)
         elements' (->> elements
                        remove-direct-link-frames
                        (apply-frame-filter frame-filter)
                        (reduce repeating-frame-reducer []))]
     (if frame-limit
       (take frame-limit elements')
       elements'))))

(defn- extract-stack-trace
  [exception options]
  (filter-stack-trace-maps (expand-stack-trace exception) options))

(defn- is-throwable?
  [v]
  (instance? Throwable v))

(defn- wrap-exception
  [^Throwable exception properties options]
  (let [throwable-property-keys (match-keys properties is-throwable?)
        nested-exception (or (->> (select-keys properties throwable-property-keys)
                                  vals
                                  (remove nil?)
                                  ;; Avoid infinite loop!
                                  (remove #(= % exception))
                                  first)
                             (.getCause exception))
        stack-trace (when-not nested-exception
                      (extract-stack-trace exception options))]
    [{:class-name (-> exception .getClass .getName)
      :message (.getMessage exception)
      ;; Don't ever want to include throwables since they will wreck the output format.
      ;; Would only expect a single throwable (either an explicit property, or as the cause)
      ;; per exception.
      :properties (apply dissoc properties throwable-property-keys)
      :stack-trace stack-trace}
     nested-exception]))

(defn- expand-exception
  [^Throwable exception options]
  (if (instance? ExceptionInfo exception)
    (wrap-exception exception (ex-data exception) options)
    (let [properties (try (into {} (bean exception))
                          (catch Throwable _ nil))
          ;; Ignore basic properties of Throwable, any nil properties, and any properties
          ;; that are themselves Throwables
          discarded-keys (concat [:suppressed :message :localizedMessage :class :stackTrace :cause]
                                 (match-keys properties nil?)
                                 (match-keys properties is-throwable?))
          retained-properties (apply dissoc properties discarded-keys)]
      (wrap-exception exception retained-properties options))))

(defn analyze-exception
  "Converts an exception into a seq of maps representing nested exceptions.
  The order reflects exception nesting; first exception is the most recently
  thrown, last is the deepest, or root, exception ... the initial exception
  thrown in a chain of nested exceptions.

  The options map is as defined by [[format-exception]].

  Each exception map contains:

  Key          | Type   | Description
  ---          |---     |---
  :class-name  | String | Name of Java class for the exception
  :message     | String | Value of the exception's message property (possibly nil)
  :properties  | String | Map of properties to (optionally) present in the exception report
  :stack-trace | Vector | Stack trace element maps (as per [[expand-stack-trace]]), or nil; only present in the root exception

  The :properties map does not include any properties that are assignable to type Throwable.

  The first property that is assignable to type Throwable (not necessarily the rootCause property)
  will be used as the nested exception (for the next map in the sequence)."
  [^Throwable e options]
  (loop [result []
         current e]
    (let [[expanded nested] (expand-exception current options)
          result' (conj result expanded)]
      (if nested
        (recur result' nested)
        result'))))

;; Shadow Clojure 1.11's version, while keeping operational in 1.10.
(defn- -update-keys
  "Builds a map where f has been applied to each key in m."
  [m f]
  (reduce-kv (fn [m k v]
               (assoc m (f k) v))
             {}
             m))

(defn- max-from
  [coll k]
  (reduce max 0 (keep k coll)))

(defn- build-stack-trace-output
  [stack-trace modern?]
  (let [source-font (:source *fonts*)
        rows (map format-stack-frame (?reverse modern? stack-trace))
        max-name-width (max-from rows :name-width)
        ;; Allow for the colon in frames w/ a line number (this assumes there's at least one)
        max-file-width (inc (max-from rows #(-> % :file length)))
        max-line-width (max-from rows #(-> % :line length))
        f (fn [{:keys [name file line repeats]}]
            (list
              [{:width max-name-width} name]
              " "
              [{:width max-file-width
                :font source-font} file]
              (when line ":")
              " "
              [{:width max-line-width} line]
              (when repeats
                [(:source *fonts*)
                 (format " (repeats %,d times)" repeats)])))]
    (interpose "\n" (map f rows))))

(defmulti exception-dispatch
          "The pretty print dispatch function used when formatting exception output (specifically, when
          printing the properties of an exception). Normally, this is the same as the simple-dispatch
          (in clojure.pprint) but can be extended for specific cases:

              (import com.stuartsierra.component.SystemMap)

              (defmethod exception-dispatch SystemMap [system-map] (print \"#<SystemMap>\"))

          This ensures that the SystemMap record, wherever it appears in the exception output,
          is represented as the string `#<SystemMap>`; normally it would print as a deeply nested
          tree of maps.

          This same approach can be adapted to any class or type whose structure is problematic
          for presenting in the exception output, whether for size and complexity reasons, or due to
          security concerns."
          class)

(defmethod exception-dispatch Object
  [object]
  (pp/simple-dispatch object))

(defmethod exception-dispatch nil
  [_]
  (pp/simple-dispatch nil))

(defn- indented-value
  [indentation s]
  (let [lines (str/split-lines s)
        sep (str "\n" (padding indentation))]
    (interpose sep lines)))

(def ^{:added "2.5.0"
       :dynamic true}
  *print-length*
  "The number of elements of collections to pretty-print; defaults to 10."
  10)

(def ^{:added "2.5.0"
       :dynamic true}
  *print-level*
  "The depth to which to pretty-printed nested collections; defaults to 5."
  5)


(defn- format-property-value
  [indentation value]
  (let [pretty-value (pp/write value
                               :stream nil
                               :length *print-length*
                               :level *print-level*
                               :dispatch exception-dispatch)]
    (indented-value indentation pretty-value)))

(defn- qualified-name
  [x]
  (if (instance? Named x)
    (let [x-ns (namespace x)
          x-name (name x)]
      (if x-ns
        (str x-ns "/" x-name)
        x-name))
    x))

(defn- replace-nil
  [x]
  (if (nil? x)
    "nil"
    x))

(defn- render-exception
  [exception-stack options]
  (let [{show-properties? :properties
         :or {show-properties? true}} options
        exception-font (:exception *fonts*)
        message-font (:message *fonts*)
        property-font (:property *fonts*)
        modern? (not *traditional*)
        max-class-name-width (max-from exception-stack #(-> % :class-name length))
        message-indent (+ 2 max-class-name-width)
        exception-f (fn [{:keys [class-name message properties]}]
                      (list
                        [{:width max-class-name-width
                          :font exception-font} class-name]
                        ":"
                        (when message
                          (list
                            " "
                            [message-font (indented-value message-indent message)]))
                        (when (and show-properties? (seq properties))
                          (let [properties' (-update-keys properties (comp replace-nil qualified-name))
                                sorted-keys (cond-> (keys properties')
                                                    (not (sorted? properties')) sort)
                                max-key-width (max-from sorted-keys length)
                                value-indent (+ 2 max-key-width)]
                            (map (fn [k]
                                   (list "\n    "
                                         [{:width max-key-width
                                           :font property-font} k]
                                         ": "
                                         [property-font
                                          (format-property-value value-indent (get properties' k))]))
                                 sorted-keys)))
                        "\n"))
        exceptions (list
                     (map exception-f (?reverse modern? exception-stack))
                     "\n")
        root-stack-trace (-> exception-stack last :stack-trace)]
    (list
      (when *traditional*
        exceptions)

      (build-stack-trace-output root-stack-trace modern?)
      "\n"

      (when modern?
        exceptions))))

(defn format-exception*
  "Contains the main logic for [[format-exception]], which simply expands
  the exception (via [[analyze-exception]]) before invoking this function."
  {:added "0.1.21"}
  [exception-stack options]
  (compose
    (render-exception exception-stack options)))

(defn format-exception
  "Formats an exception, returning a single large string.

  By default, includes the stack trace, with no frame limit.

  The options map may have the following keys:

  Key          | Description
  ---          |---
  :filter      | The stack frame filter, which defaults to [[*default-stack-frame-filter*]]
  :properties  | If true (the default) then properties of exceptions will be output
  :frame-limit | If non-nil, the number of stack frames to keep when outputting the stack trace of the deepest exception

  Output may be traditional or modern, as controlled by [[*traditional*]].
  Traditional is the typical output order for Java: the stack of exceptions comes first (outermost to
  innermost) followed by the stack trace of the innermost exception, with the frames
  in order from deepest to most shallow.

  Modern output is more readable; the stack trace comes first and is reversed: shallowest frame to most deep.
  Then the exception stack is output, from the root exception to the outermost exception.
  The modern output order is more readable, as it puts the most useful information together at the bottom, so that
  it is not necessary to scroll back to see, for example, where the exception occurred.

  The default is modern.

  The stack frame filter is passed the map detailing each stack frame
  in the stack trace, and must return one of the following values:

  Value      | Description
  ---        |---
  :show      | The normal state; display the stack frame
  :hide      | Prevents the frame from being displayed, as if it never existed
  :omit      | Replaces the frame with a \"...\" placeholder
  :terminate | Hides the frame AND all later frames

  Multiple consecutive :omits will be collapsed to a single line; use :omit for \"uninteresting\" stack frames.

  The default filter is [[*default-frame-filter*]].  An explicit filter of nil will display all stack frames.

  Repeating lines are collapsed to a single line, with a repeat count. Typically, this is the result of
  an endless loop that terminates with a StackOverflowException.

  When set, the frame limit is the number of stack frames to display; if non-nil, then some outermost
  stack frames may be omitted. It may be set to 0 to omit the stack trace entirely (but still display
  the exception stack).  The frame limit is applied after the frame filter (which may hide or omit frames) and
  after repeating stack frames have been identified and coalesced ... :frame-limit is really the number
  of _output_ lines to present.

  Properties of exceptions will be output using Clojure's pretty-printer, but using
  this namespace's versions of [[*print-length*]] and [[*print-level*]], which default to
  10 and 5, respectively.

  The `*fonts*` var contains a map from output element names (as :exception or :clojure-frame) to
  a font def used with [[compose]]; this allows easy customization of the output."
  (^String [exception]
   (format-exception exception nil))
  (^String [exception options]
   (format-exception* (analyze-exception exception options) options)))

(defn print-exception
  "Formats an exception via [[format-exception]], then prints it to `*out*`.  Accepts the same options as `format-exception`."
  ([exception]
   (print-exception exception nil))
  ([exception options]
   (print (format-exception exception options))
   (flush)))

(defn- assemble-final-stack
  [exceptions stack-trace stack-trace-batch options]
  (let [*cache (volatile! {})
        stack-trace' (-> (map #(transform-stack-trace-element current-dir-prefix *cache %)
                              (into stack-trace-batch stack-trace))
                         (filter-stack-trace-maps options))
        x (-> exceptions count dec)]
    (assoc-in exceptions [x :stack-trace] stack-trace')))

(def ^:private re-exception-start
  "The start of an exception, possibly the outermost exception."
  #"(Caused by: )?(\w+(\.\w+)*): (.*)"
  ; Group 2 - exception name
  ; Group 4 - exception message
  )

(def ^:private re-stack-frame
  ;; Sometimes the file name and line number are replaced with "Unknown source"
  #"\s+at ([a-zA-Z_.$\d<>]+)\(((.+):(\d+))?.*\).*"
  ; Group 1 - class and method name
  ; Group 3 - file name (or nil)
  ; Group 4 - line number (or nil)
  )

(defn- add-message-text
  [exceptions line]
  (let [x (-> exceptions count dec)]
    (update-in exceptions [x :message]
               str \newline line)))

(defn- add-to-batch
  [stack-trace-batch ^String class-and-method ^String file-name ^String line-number]
  (try
    (let [x (.lastIndexOf class-and-method ".")
          class-name (subs class-and-method 0 x)
          method-name (subs class-and-method (inc x))
          element (StackTraceElement. class-name
                                      method-name
                                      file-name
                                      (if line-number
                                        (Integer/parseInt line-number)
                                        -1))]
      (conj stack-trace-batch element))
    (catch Throwable t
      (throw (ex-info "Unable to create StackTraceElement."
                      {:class-and-method class-and-method
                       :file-name file-name
                       :line-number line-number}
                      t)))))

(defn parse-exception
  "Given a chunk of text from an exception report (as with `.printStackTrace`), attempts to
  piece together the same information provided by [[analyze-exception]].  The result
  is ready to pass to [[write-exception*]].

  This code does not attempt to recreate properties associated with the exceptions; in most
  exception's cases, this is not necessarily written to the output. For clojure.lang.ExceptionInfo,
  it is hard to distinguish the message text from the printed exception map.

  The options are used when processing the stack trace and may include the :filter and :frame-limit keys.

  Returns a sequence of exception maps; the final map will include the :stack-trace key (a vector
  of stack trace element maps).  The exception maps are ordered outermost to innermost (that final map
  is the root exception).

  This should be considered experimental code; there are many cases where it may not work properly.

  It will work quite poorly with exceptions whose message incorporates a nested exception's
  .printStackTrace output. This happens too often with JDBC exceptions, for example."
  {:added "0.1.21"}
  [exception-text options]
  (loop [state :start
         lines (str/split-lines exception-text)
         exceptions []
         stack-trace []
         stack-trace-batch []]
    (if (empty? lines)
      (assemble-final-stack exceptions stack-trace stack-trace-batch options)
      (let [[line & more-lines] lines]
        (condp = state

          :start
          (let [[_ _ exception-class-name _ exception-message] (re-matches re-exception-start line)]
            (when-not exception-class-name
              (throw (ex-info "Unable to parse start of exception."
                              {:line line
                               :exception-text exception-text})))

            ;; The exception message may span a couple of lines, so check for that before absorbing
            ;; more stack trace
            (recur :exception-message
                   more-lines
                   (conj exceptions {:class-name exception-class-name
                                     :message exception-message})
                   stack-trace
                   stack-trace-batch))

          :exception-message
          (if (re-matches re-stack-frame line)
            (recur :stack-frame lines exceptions stack-trace stack-trace-batch)
            (recur :exception-message
                   more-lines
                   (add-message-text exceptions line)
                   stack-trace
                   stack-trace-batch))

          :stack-frame
          (let [[_ class-and-method _ file-name line-number] (re-matches re-stack-frame line)]
            (if class-and-method
              (recur :stack-frame
                     more-lines
                     exceptions
                     stack-trace
                     (add-to-batch stack-trace-batch class-and-method file-name line-number))
              (recur :skip-more-line
                     lines
                     exceptions
                     ;; With the weird ordering of the JDK, what we see is
                     ;; a batch of entries that actually precede frames from earlier
                     ;; in the output (because JDK tries to present the exceptions outside in).
                     ;; This inner exception and its abbreviated stack trace represents
                     ;; progress downward from the previously output exception.
                     (into stack-trace-batch stack-trace)
                     [])))

          :skip-more-line
          (if (re-matches #"\s+\.\.\. \d+ (more|common frames omitted)" line)
            (recur :start more-lines
                   exceptions stack-trace stack-trace-batch)
            (recur :start lines
                   exceptions stack-trace stack-trace-batch)))))))

(defn format-stack-trace-element
  "Formats a stack trace element into a single string identifying the Java method or Clojure function being executed."
  {:added "3.2.0"}
  [^StackTraceElement e]
  (let [{:keys [class method names]} (transform-stack-trace-element current-dir-prefix (volatile! {}) e)]
    (if (empty? names)
      (str class "." method)
      (->> names counted-terms (map counted-frame-name) (str/join "/")))))
