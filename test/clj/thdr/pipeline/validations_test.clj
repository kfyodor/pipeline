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
    (is (= ["there was an error"] (m/extract res))))

  (let [res (exception->validation (errored-fn) :custom-msg)]
    (is (v/fail? res))
    (is (= [:custom-msg] (m/extract res))))

  (let [res (exception->validation (exc/try-on :ok))]
    (is (v/ok? res))
    (is (= :ok (m/extract res))))

  (let [res (exception->validation (exc/try-on :ok) :nevermind)]
    (is (v/ok? res))
    (is (= :ok (m/extract res))))

  (is (thrown? java.lang.AssertionError (exception->validation :ok))))

(deftest test-validate-in
  (let [validation (validate-in [:age] valid-age?)]
    (let [res (validation valid-user)]
      (is (v/ok? res))
      (is (= valid-user (m/extract res))))

    (let [res (validation invalid-user)]
      (is (v/fail? res))
      (is (= [:invalid-age] (m/extract res))))))

(deftest test-compose-validations
  (let [res (user-validations valid-user)]
    (is (v/ok? res))
    (is (= valid-user (m/extract res))))

  (let [res (user-validations invalid-user)]
    (is (v/fail? res))
    (is (= #{:invalid-age
             :password-too-short
             :confirmation-doesnt-match}
           (-> res m/extract set)))))

(deftest test-run-validations
  (let [res (run-validations valid-user user-validations)]
    (is (e/right? res))
    (is (= valid-user (m/extract res))))

  (let [res (run-validations invalid-user user-validations)]
    (is (e/left? res))
    (is (= #{:invalid-age
             :password-too-short
             :confirmation-doesnt-match}
           (-> res m/extract set)))))
