(ns cljang.core
  (:import
   (org.bytedeco.javacpp Loader clang)))

#_(Loader/load clang)

(defn ^String clang-version
  []
  (-> (clang/clang_getClangVersion) .getString))
