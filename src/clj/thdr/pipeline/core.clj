(ns thdr.pipeline.core
  (:require [cats.builtin]
            [cats.core :as m]
            [cats.monad
             [either :refer [left right left? either?] :as either]
             [exception :as exc]]
            [cats.context :as ctx]
            [cats.protocols :as proto]))

(defn exception->either
  "Transforms [cats.monad.exception/Exception] to
   [cats.monad.either/Either]"
  ([e]
   (exception->either e nil))
  ([e msg]
   {:pre (exc/exception? e)}
   (let [val (m/extract e)]
     (if (exc/success? e)
       (right val)
       (left (or msg (.getMessage val)))))))

(defmacro try-either [& forms]
  "Runs code which can throw an error
  and tranforms the result to Either"
  `(exception->either
    (exc/try-on ~@forms)))

(defn run-either
  "Similar to [cats.monad.either/branch]
  but with flipped arguments."
  [e rf lf]
  {:pre [(either? e)]}
  (if (left? e)
    (lf (proto/-extract e))
    (rf (proto/-extract e))))

(defmacro pipeline->
  "Works like [clojure.core/->] threading macro
   but for functions which return an Either monad.

   Expects first form to return an instance of Either.
   Use other monadic values carefully (read: don't use)
   since `funcool/cats` has dynamic context in `mlet`
   (I think there're working on this)  and this can lead
   to unexpected results.

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
  argument (like [clojure.core/->>])"
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
