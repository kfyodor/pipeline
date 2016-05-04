(ns thdr.pipeline.schema
  (:require [cats.builtin]
            [cats.monad.either :as either]
            [schema.core :as schema]
            [schema.coerce :as coerce]
            [schema.utils :as utils]))

(defn schema-check
  "Runs [schema.core/check] agaist provided
  schema and a value.

  Accepts an optional function for errors tranformation"
  ([data schema] (schema-check data schema identity))
  ([data schema t-fn]
   (if-let [errors (schema/check schema data)]
     (either/left (t-fn errors))
     (either/right data))))

(defn schema-coerce
  "Coerces and checks data against provided schema. Returns Either.

   *Options*:
   **t-fn**: a function for transforming error data. Default is [clojure.core/identity]

   **coercer**: a schema coercer. Defalut is [schema.coerce/json-coercion-matcher]

   **select-keys?**: Get schema keys, then check + coerce only selected keys in provided map.
   Caveat: won't work if schema contains [schema.core/optional-key], but anyway
   you don't need `optional-key` with `select-keys?` set to true."
  ([data schema & {:keys [t-fn coercer select-keys?]
                   :or {t-fn identity
                        coercer coerce/json-coercion-matcher
                        select-keys? false}}]
   (let [d (if select-keys?
             (->> (keys schema) (select-keys data))
             data)
         check (coerce/coercer schema coercer)
         res   (check d)]
     (if-let [err (and (utils/error? res)
                       (utils/error-val res))]
       (either/left (t-fn err))
       (either/right (if select-keys?
                       (merge data res)
                       res))))))
