(ns thdr.pipeline.core-test
  (:require [clojure.test :refer :all]
            [thdr.pipeline.core :refer :all]
            [thdr.pipeline.test-data :refer :all]
            [cats.core :as m]
            [cats.monad.either :as e]
            [cats.monad.exception :as exc]))

(deftest test-exception->either
  (let [res (exception->either (exc/try-on (errored-fn)))]
    (is (e/left? res))
    (is (= "there was an error" (m/extract res))))

  (let [res (exception->either (exc/try-on :ok))]
    (is (e/right? res))
    (is (= :ok (m/extract res)))))

(deftest test-try-either
  (let [res (try-either (errored-fn))]
    (is (e/left? res))
    (is (= "there was an error" (m/extract res))))

  (let [res (try-either :ok)]
    (is (e/right? res))
    (is (= :ok (m/extract res)))))

(deftest test-run-either
  (let [res (run-either (e/right :ok) (fn [v] v) (fn [e] :nope))]
    (is (= :ok res)))

  (let [res (run-either (e/left :err) (fn [v] :nope) (fn [e] e))]
    (is (= :err res))))


(defn minus [a b]
  (e/right (- a b)))

(deftest test-pipeline->
  (let [res (pipeline-> (e/right 2) (minus 1))]
    (is (= (>>=-> (e/right 2) (minus 1)) res))
    (is (= (e/right 1) res)))

  (let [res (pipeline-> (e/left "err") (minus 1))]
    (is (= (e/left "err") res))))

(deftest test-pipeline->>
  (let [res (pipeline->> (e/right 2) (minus 1))]
    (is (= (>>=->> (e/right 2) (minus 1)) res))
    (is (= (e/right -1) res)))

  (let [res (pipeline->> (e/left "err") (minus 1))]
    (is (= (e/left "err") res))))
