(ns thdr.pipeline.core
  (:require [cats.builtin]
            [cats.core :as m]
            [cats.monad.either :refer [left right left? either?] :as either]
            [cats.monad.exception :as exc]
            [cats.context :as ctx]
            [cats.protocols :as proto]))

(defn monadic? [a]
  (satisfies? proto/Contextual a))

(defn exception->either
  "Transforms [cats.monad.exception/Exception] to
   [cats.monad.either/Either]"
  ([e]
   (exception->either e nil))
  ([e msg]
   (let [val (m/extract e)]
     (if (exc/success? e)
       (right val)
       (left (or msg (.getMessage val)))))))

(defmacro try-either [& forms]
  "Run code which can throw exception
  and tranform them to either"
  `(exception->either
    (exc/try-on ~@forms)))

(defn run-either
  "Similar to [cats.monad.either/branch]
  but with flipped arguments"
  [e rf lf]
  {:pre [(either? e)]}
  (if (left? e)
    (lf (proto/-extract e))
    (rf (proto/-extract e))))

(defmacro pipeline->
  "Works like [clojure.core/->] threading macro
   but for functions which return Either monad.

   Threads expressions through [cats.core/mlet] macro
   (i.e. chain of monadic binds, or >>=) and returns
   [cats.monad.either/right] if computation was
   successful, or [cats.monad.either/left] if
   computation failed at some point."
  [x & forms]
  (let [sym (gensym)
        init [sym x]
        f (fn [bindings form]
            (conj bindings
                  sym
                  (if (seq form)
                    `(~(first form) ~sym ~@(next form))
                    (list form sym))))
        bindings (reduce f init forms)]
    `(ctx/with-context either/context
       (m/mlet ~bindings (m/return ~sym)))))

(defmacro >>=->
  "Pipeline-> = chain of monadic binds (>>=) + [clojure.core/->]"
  [& forms]
  `(pipeline-> ~@forms))

(defmacro pipeline->>
  "Same as [pipeline->] but threads through the last
  arguments (like [clojure.core/->>])"
  [x & forms]
  (let [sym (gensym)
        init [sym x]
        f (fn [bindings form]
            (conj bindings
                  sym
                  (if (seq form)
                    `(~@form ~sym)
                    (list form sym))))
        bindings (reduce f init forms)]
    `(ctx/with-context either/context
       (m/mlet ~bindings (m/return ~sym)))))

(defmacro >>=->> [& forms]
  "See [thdr.pipeline.core/>>=->]"
  `(pipeline->> ~@forms))
