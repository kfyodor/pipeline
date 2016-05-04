(ns thdr.pipeline.validations
  (:require [cats.builtin]
            [cats.core :as m]
            [cats.monad.either :as e]
            [cats.monad.exception :as exc]
            [cats.context :as ctx]
            [cats.applicative.validation :as v]))

(defn exception->validation
  "Transforms Exception to Validation"
  ([e]
   (exception->validation e nil))
  ([e msg]
   (let [val (m/extract e)]
     (if (exc/success? e)
       (v/ok val)
       (v/fail [(or msg (.getMessage val))])))))

(defn validate-in [key validation-fn]
  "Validates associative structure's
  value by provided key (or path) and a validation function.
  Returns provided data if validation was successful."
  (fn [data]
    {:pre [(associative? data)]}
    (let [key (if (vector? key) key [key])
          val (get-in data key)
          res (validation-fn val)]
      (if (v/fail? res)
        res
        (v/ok data)))))

(defn- wrap-validation [validation]
  "Wraps a validation fn (v -> Validation v)
  into a middleware-like function which accepts
  **next-validation** and returns a result of [cats.core/fapply]
  called with the provided validation and the next-validation."
  (fn [next-validation]
    (fn [data]
      (ctx/with-context v/context
        (m/<*> (m/pure (fn [_]
                         (fn [_]
                           data)))
               (validation data)
               (next-validation data))))))

(defn compose-validations
  "Given a collection of functions (v -> Validation v),
  composes them into a single (v -> Validation v) one."
  [first & rest]
  (let [composed (reduce #(wrap-validation (%1 %2))
                         (wrap-validation first)
                         rest)]
    (composed (fn [_] (v/ok _)))))

(def >==> compose-validations)

(defn run-validations
  "Applies validation-fn (or validations composition
  made by [>==>]) and transforms it to [cats.monads.either/either]."
  [v validation-fn]
  (let [res (validation-fn v)
        val (m/extract res)]
    (if (v/fail? res)
      (e/right val)
      (e/left val))))
