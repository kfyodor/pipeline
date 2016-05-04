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
    (is (= (m/extract res) "there was an error")))

  (let [res (exception->either (exc/try-on :ok))]
    (is (e/right? res))
    (is (= (m/extract res) :ok))))

(deftest test-try-either
  (let [res (try-either (errored-fn))]
    (is (e/left? res))
    (is (= (m/extract res) "there was an error")))

  (let [res (try-either :ok)]
    (is (e/right? res))
    (is (= (m/extract res) :ok))))

(deftest test-run-either
  (let [res (run-either (e/right :ok) (fn [v] v) (fn [e] :nope))]
    (is (= res :ok)))

  (let [res (run-either (e/left :err) (fn [v] :nope) (fn [e] e))]
    (is (= res :err))))


(defn minus [a b]
  (e/right (- a b)))

(deftest test-pipeline->
  (let [res (pipeline-> (e/right 2) (minus 1))]
    (is (= res (>>=-> (e/right 2) (minus 1))))
    (is (= res (e/right 1))))

  (let [res (pipeline-> (e/left "err") (minus 1))]
    (is (= res (e/left "err")))))

(deftest test-pipeline->>
  (let [res (pipeline->> (e/right 2) (minus 1))]
    (is (= res (>>=->> (e/right 2) (minus 1))))
    (is (= res (e/right -1))))

  (let [res (pipeline->> (e/left "err") (minus 1))]
    (is (= res (e/left "err")))))
