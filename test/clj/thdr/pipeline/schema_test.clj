(ns thdr.pipeline.schema-test
  (:require [clojure.test :refer :all]
            [thdr.pipeline.schema :refer :all]
            [thdr.pipeline.test-data :refer :all]
            [schema.core :as s]
            [cats.monad.either :as e]
            [cats.core :as m])
  (:import [schema.utils ErrorContainer]))

;; TODO: coercer option test

(deftest schema-check-test
  (testing "correct params"
    (let [res (schema-check correct-attrs TestUser)]
      (is (e/right? res))
      (is (= correct-attrs (m/extract res)))))

  (testing "wrong params"
    (let [attrs {:id 123}
          res (schema-check wrong-attrs TestUser)]
      (is (e/left? res))
      (is (= [:id :password :age] (keys (m/extract res)))))))

(deftest schema-coerce-test
  (testing "correct attrs with id:UUID"
    (let [res (schema-coerce correct-attrs TestUser)]
      (is (e/right? res))
      (is (= correct-attrs (m/extract res)))))

  (testing "correct attrs with id:String"
    (let [res (schema-coerce correct-attrs-str TestUser)]
      (is (e/right? res))
      (is (= correct-attrs (m/extract res)))))

  (testing "wrong attrs"
    (let [res (schema-coerce wrong-attrs TestUser)]
      (is (e/left? res))
      (is (= [:id :password :age] (keys (m/extract res))))))

  (testing "select-keys? set to true"
    (testing "correct attrs"
      (let [res (schema-coerce correct-attrs IdSchema :select-keys? true)]
        (is (e/right? res))
        (is (= correct-attrs (m/extract res)))))

    (testing "wrong attrs"
      (let [res (schema-coerce wrong-attrs IdSchema :select_keys? true)]
        (is (e/left? res))
        (is (= [:id] (keys (m/extract res)))))))

  (testing "t-fn provided"
    (let [res (schema-coerce wrong-attrs TestUser :t-fn keys)]
      (is (e/left? res))
      (is (= [:id :password :age] (m/extract res))))))
