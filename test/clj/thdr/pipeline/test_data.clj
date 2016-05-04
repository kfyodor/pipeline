(ns thdr.pipeline.test-data
  (:require [thdr.pipeline
             [validations :refer :all]
             [core :refer :all]
             [schema :refer :all]]
            [cats.monad
             [either :as e]
             [exception :as exc]]
            [cats.applicative.validation :as v]
            [schema.core :as s])
  (:import [java.util UUID]))

(def uuid (UUID/randomUUID))
(def uuid-str (str uuid))

;;;;;;;;;;;;;;;;;;;; schema ;;;;;;;;;;;;;;;;;;;;

(s/defschema IdSchema
  {:id UUID})

(s/defschema TestUser
  (merge IdSchema
         {:password s/Str
          :age s/Int}))

(def correct-attrs
  {:id uuid
   :password "123456"
   :age 22})

(def correct-attrs-str
  (-> correct-attrs
      (assoc-in [:id] uuid-str)))

(def wrong-attrs
  {:id 123})

;;;;;;;;;;;;;;;;;;;; validations ;;;;;;;;;;;;;;;;;;;;

(defn errored-fn []
  (exc/try-on
   (throw (Exception. "there was an error"))))

(def valid-user
  {:age 21
   :password "123456"
   :password_confirmation "123456"})

(def invalid-user
  {:age 17
   :password "123"
   :password_confirmation "1234"})

(defn valid-age? [age]
  (if (>= age 21)
    (v/ok age)
    (v/fail [:invalid-age])))

(defn password-long-enough? [password]
  (if (>= (count password) 6)
    (v/ok password)
    (v/fail [:password-too-short])))

(defn confirmation-matches?
  [{:keys [password password_confirmation] :as user}]
  (if (= password password_confirmation)
    (v/ok user)
    (v/fail [:confirmation-doesnt-match])))

(def user-validations (>==> (validate-in [:age] valid-age?)
                            (validate-in [:password] password-long-enough?)
                            confirmation-matches?))
