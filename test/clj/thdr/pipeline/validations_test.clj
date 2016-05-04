(ns thdr.pipeline.validations-test
  (:require [clojure.test :refer :all]
            [thdr.pipeline.validations :refer :all]
            [cats.core :as m]
            [cats.monad.exception :as exc]
            [cats.monad.either :as e]
            [cats.applicative.validation :as v]))

;;;;;;;;;;;;;;;;;;;; test functions & data ;;;;;;;;;;;;;;;;;;;;

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

;;;;;;;;;;;;;;;;;;;; tests ;;;;;;;;;;;;;;;;;;;;

(deftest test-exception->validation
  (let [res (exception->validation (errored-fn))]
    (is (v/fail? res))
    (is (= (m/extract res) ["there was an error"])))

  (let [res (exception->validation (errored-fn) :custom-msg)]
    (is (v/fail? res))
    (is (= (m/extract res) [:custom-msg])))

  (let [res (exception->validation (exc/try-on :ok))]
    (is (v/ok? res))
    (is (= (m/extract res) :ok)))

  (let [res (exception->validation (exc/try-on :ok) :nevermind)]
    (is (v/ok? res))
    (is (= (m/extract res) :ok)))

  (is (thrown? java.lang.AssertionError (exception->validation :ok))))

(deftest test-validate-in
  (let [validation (validate-in [:age] valid-age?)]
    (let [res (validation valid-user)]
      (is (v/ok? res))
      (is (= (m/extract res) valid-user)))

    (let [res (validation invalid-user)]
      (is (v/fail? res))
      (is (= (m/extract res) [:invalid-age])))))

(deftest test-compose-validations
  (let [res (user-validations valid-user)]
    (is (v/ok? res))
    (is (m/extract res) valid-user))

  (let [res (user-validations invalid-user)]
    (is (v/fail? res))
    (is (= (-> res m/extract set) #{:invalid-age
                                    :password-too-short
                                    :confirmation-doesnt-match}))))

(deftest test-run-validations
  (let [res (run-validations valid-user user-validations)]
    (is (e/right? res))
    (is (= (m/extract res) valid-user)))

  (let [res (run-validations invalid-user user-validations)]
    (is (e/left? res))
    (is (= (-> res m/extract set) #{:invalid-age
                                    :password-too-short
                                    :confirmation-doesnt-match}))))
