(ns thdr.pipeline.validations
  "Various functions for validating data structures and
  composing and transforming validation functionas based on
  Validation applicative functors."
  (:require [cats.builtin]
            [cats.core :as m]
            [cats.monad.either :as e]
            [cats.monad.exception :as exc]
            [cats.context :as ctx]
            [cats.applicative.validation :as v]))

(defn exception->validation
  "Transforms Exception to Validation.
  Accepts custom error message as a second argument."
  ([e]
   (exception->validation e nil))
  ([e msg]
   {:pre [(exc/exception? e)]}
   (let [val (m/extract e)]
     (if (exc/success? e)
       (v/ok val)
       (v/fail [(or msg (.getMessage val))])))))

(defn validate-in [key validation-fn]
  "A higher-order function which creates a function (a -> Validation a)
  which validates an associative data structure's value at provided
  key (or path) by validation-fn.

  Inner function returns whole data structure (wrapped in Ok)
  if validation was successful or Fail(errors) if validation failed."
  (fn [data]
    {:pre [(associative? data)]}
    (let [key (if (vector? key) key [key])
          val (get-in data key)
          res (validation-fn val)]
      (if (v/fail? res)
        res
        (v/ok data)))))

(defn- wrap-validation [validation-fn]
  "Wraps `validation-fn` (v -> Validation v)
  into a middleware-like structure which accepts
  `next-validation-fn` and returns a result of [cats.core/fapply]
  called with provided `validation-fn` and `next-validation-fn`.

  Note: Since this function is for data validation only, it
  ignores any (v/ok ..) values returned from provided validation."
  (fn [next-validation-fn]
    (fn [data]
      (ctx/with-context v/context
        (m/<*> (m/pure (fn [_]
                         (fn [_]
                           data)))
               (validation-fn data)
               (next-validation-fn data))))))

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
      (e/left val)
      (e/right val))))
