(ns example
  "An example on how you could use `pipeline` lib in you app.
  This is not intended to be compiled and run."
  (:require [cats.core :refer [return]]
            [cats.applicative.validation :refer [fail ok]]
            [schema.core :refer [defschema]]
            [example.db :as db]
            [example.serialization :as serialization]
            [io.pedestal.http :refer [response status]]
            [thdr.pipeline.core :refer [try-either pipeline->]]
            [thdr.pipeline.validations :refer [run-validations >==> validate-in]]
            [thdr.pipeline.schema :refer [schema-coerce]]))

;;;;;;;;;;;;;;;;;;;; Schemas ;;;;;;;;;;;;;;;;;;;;

(defschema UserSchema
  {:id java.util.UUID
   :password Str
   :password_confirmation Str
   :age Int})

;;;;;;;;;;;;;;;;;;;; Validation fns ;;;;;;;;;;;;;;;;;;;;

​
(defn- age-ok?
  [age]
  (if (< age 21)
    (fail [:age<21])
    (ok age)))
​
(defn- password-long-enough?
  [password]
  (if (< (count password) 8)
    (fail [:password-too-short])
    (ok password)))
​
(defn- password-confirmation-matches?
  [{:keys [password password-confirmation] :as user}]
  (if (= password password-confirmation)
    (ok user)
    (fail [:password-confirmation-doesnt-match])))
​
(def ^{:doc "Validations composition"
       :private true} user-validations
  (>==> (validate-in [:age] age-ok?)
        (validate-in [:password])
        password-confirmation-matches?))

;;;;;;;;;;;;;;;;;;;; Handler ;;;;;;;;;;;;;;;;;;;;
​​
(defn- user->db
  "Persist"
  [attrs db-conn]
  (try-either
   (->> params
        (serialization/serialize-for-update)
        (db/update-user<! db-conn))))
​
(defn- make-error-response [error]
  "Match errors in order to return a proper response."
  (let [[resp s] (cond
                   (= :not-found error) [{} 404]
                   (map? error)         [{} 422])] ;; ... etc
    (-> (response resp)
        (status s))))
​
(defn update-user
  "A Pedestal handler for updating users."
  [{:keys [params db-conn] :as req}]
  (run-either
   (pipeline-> (return (params :user))
               (schema-coerce UserSchema)
               (run-validations user-validations)
               (user->db db-conn))
   ;; and-then
   (fn [user]
     (-> (response (serialization/serialize user))
         (status 201)))

   ;; or-else
   (fn [error]
     (-> (make-error-response error)))))
