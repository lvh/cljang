(defproject cljang "0.3.0"
  :description "Clang for Clojure"
  :url "https://github.com/lvh/cljang"
  :license {:name "EPL-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.bytedeco.javacpp-presets/llvm "7.0.1-1.4.4"]
                 [org.bytedeco.javacpp-presets/llvm-platform "7.0.1-1.4.4"]
                 [camel-snake-kebab "0.4.0"]]
  :repositories [["releases" :clojars]
                 ["snapshots" :clojars]]
  :main ^:skip-aot cljang.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
