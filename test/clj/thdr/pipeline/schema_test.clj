(ns thdr.pipeline.schema-test
  (:require [clojure.test :refer :all]
            [thdr.pipeline.schema :refer :all]
            [schema.core :as s]
            [cats.monad.either :as e]
            [cats.core :as m])
  (:import [schema.utils ErrorContainer]))

;; TODO: coercer option test

(s/defschema IdSchema
  {:id java.util.UUID})

(s/defschema TestUser
  (merge IdSchema
         {:password s/Str
          :age s/Int}))

(def correct-attrs
  {:id (java.util.UUID/randomUUID)
   :password "123456"
   :age 22})

(def correct-attrs-str
  (-> correct-attrs
      (update-in [:id] str)))

(def wrong-attrs
  {:id 123})

(deftest schema-check-test
  (testing "correct params"
    (let [res (schema-check correct-attrs TestUser)]
      (is (e/right? res))
      (is (= (m/extract res) correct-attrs))))

  (testing "wrong params"
    (let [attrs {:id 123}
          res (schema-check wrong-attrs TestUser)]
      (is (e/left? res))
      (is (= (keys (m/extract res)) [:id :password :age])))))

(deftest schema-coerce-test
  (testing "correct attrs with id:UUID"
    (let [res (schema-coerce correct-attrs TestUser)]
      (is (e/right? res))
      (is (m/extract res) correct-attrs)))

  (testing "correct attrs with id:String"
    (let [res (schema-coerce correct-attrs-str TestUser)]
      (is (e/right? res))
      (is (m/extract res) correct-attrs)))

  (testing "wrong attrs"
    (let [res (schema-coerce wrong-attrs TestUser)]
      (is (e/left? res))
      (is (= (keys (m/extract res)) [:id :password :age]))))

  (testing "select-keys? set to true"
    (testing "correct attrs"
      (let [res (schema-coerce correct-attrs IdSchema :select-keys? true)]
        (is (e/right? res))
        (is (= (m/extract res) correct-attrs))))

    (testing "wrong attrs"
      (let [res (schema-coerce wrong-attrs IdSchema :select_keys? true)]
        (is (e/left? res))
        (is (= (keys (m/extract res)) [:id])))))

  (testing "t-fn provided"
    (let [res (schema-coerce wrong-attrs TestUser :t-fn keys)]
      (is (e/left? res))
      (is (= (m/extract res) [:id :password :age])))))
