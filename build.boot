(set-env! :resource-paths #{"src/clj"}
          :dependencies '[[funcool/cats "1.2.1"        :scope "provided"]
                          [prismatic/schema "1.0.5"    :scope "provided"]
                          [org.clojure/clojure "1.8.0" :scope "test"]
                          [boot-codox "0.9.5"          :scope "test"]
                          [adzerk/bootlaces "0.1.13"   :scope "test"]
                          [adzerk/boot-test "1.1.1"    :scope "test"]])

(require '[adzerk.boot-test :as test]
         '[adzerk.bootlaces :refer :all]
         '[codox.boot :refer [codox]])

(def +version+ "0.1.1-SNAPSHOT")
(bootlaces! +version+ :dont-modify-paths? true)

(task-options!
 pom {:project     'io.thdr/pipeline
      :version     +version+
      :description "Monadic workflow for Clojure apps"
      :url         "https://github.com/konukhov/pipeline"
      :scm         {:url "https://github.com/konukhov/pipeline"}
      :license     {"Eclipse Public License"
                    "http://www.eclipse.org/legal/epl-v10.html"}})

(deftask with-test-paths []
  (merge-env! :source-paths #{"test/clj"})
  identity)

(deftask test []
  (comp (with-test-paths)
        (test/test)))

(deftask doc []
  (require '[cljs.core])
  (comp (codox :name "pipeline"
               :language :clojurescript
               :output-path "./doc")
        (target)))
