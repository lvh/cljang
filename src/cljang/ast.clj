(ns cljang.ast
  (:require [clojure.datafy :as datafy]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [camel-snake-kebab.core :as csk]
            [clojure.zip :as zip])
  (:import (org.bytedeco.javacpp clang BytePointer IntPointer PointerPointer)))

;; https://github.com/bytedeco/javacpp-presets/blob/master/llvm/samples/src/main/java/org/bytedeco/javacpp/samples/clang/ClangASTVisitorExample.java

(defn ^:private intbool
  [x]
  (if x 1 0))

(defn create-index!
  [exclude-declarations-from-pch display-diagnostics]
  (clang/clang_createIndex
   (intbool exclude-declarations-from-pch)
   (intbool display-diagnostics)))

(defn cursor
  [cursor]
  (-> cursor clang/clang_getCursorSpelling .getString))

(defn cursor-kind
  [cursor]
  (-> cursor clang/clang_getCursorKind clang/clang_getCursorKindSpelling .getString))

(defn cursor-type
  [cursor]
  (-> cursor clang/clang_getCursorType clang/clang_getTypeSpelling .getString))

(defn cursor-type-kind
  [cursor]
  (-> cursor clang/clang_getCursorType .kind clang/clang_getTypeKindSpelling .getString))

(defn cursor-details
  [cursor]
  (->> (for [v [#'cursor #'cursor-kind #'cursor-type #'cursor-type-kind]]
         [(keyword "clang" (-> v meta :name)) (v cursor)])
       (into {})))

(defn parse
  [path]
  (let [idx (create-index! false true)
        unit (org.bytedeco.javacpp.clang$CXTranslationUnit.)
        res (clang/clang_parseTranslationUnit2
             idx (BytePointer. path)
             nil 0 ;; command line args
             nil 0 ;; unsaved files
             clang/CXTranslationUnit_None
             unit)]
    (if (= res clang/CXError_Success)
      unit
      ;; TODO: translate the cxerror code
      (throw (ex-info "parse failure" {::cxerror res})))))

(defn ^:private enum-fields
  [base prefix]
  (let [underscored-prefix (str (name prefix) "_")
        is-enum-member? #(-> % key name (str/starts-with? underscored-prefix))
        members (->> base datafy/datafy :members)]
    (eduction (filter is-enum-member?) (map val) members)))

(defn ^:private enum-vals->kw
  [base prefix]
  (map
   (juxt
    :name
    (comp csk/->kebab-case-keyword last #(str/split % #"_") name :name))
   (enum-fields base prefix)))

(-> (enum-fields clang "CXChildVisit") first first)

(defn ^:private ->visitor
  "Given a fn, wrap it to be a CXCursorVisitor."
  [f]
  (proxy [org.bytedeco.javacpp.clang$CXCursorVisitor] []
    (call [cursor parent client-data]
      (case (f cursor parent)
        ::break clang/CXChildVisit_Break
        ::continue clang/CXChildVisit_Continue
        ::recurse clang/CXChildVisit_Recurse
        clang/CXChildVisit_Recurse))))

(defn traverse!
  [f unit]
  (let [root (clang/clang_getTranslationUnitCursor unit)
        level (org.bytedeco.javacpp.clang$CXClientData. (IntPointer. (int-array 1)))]
    (clang/clang_visitChildren root (->visitor f) level)))

(extend-protocol clojure.core.protocols/Datafiable
  org.bytedeco.javacpp.clang$CXTranslationUnit
  (datafy [u]
    ))

(defn unit->data
  [unit]
  (let [state (atom [])]
    (traverse! (fn [cursor parent]
                 (swap! state conj cursor)
                 ::recurse)
               unit)
    @state))

#_(->> (parse "/home/user/Projects/libsodium/src/libsodium/include/sodium.h")
       unit->data
       first
       (def c))

(#'cursor c)
(#'cursor-type c)

(#'cursor-kind c)
(#'cursor-type-kind c)

#_(cursor-details c)


#_(->> clang datafy :members (filter #(-> % first)))


(defn map-zipper
  [m]
  (zip/zipper
   (fn [x]
     (or (map? x)
         (map? (nth x 1))))
   (fn [x]
     (seq (if (map? x) x (nth x 1))))
   (fn [x children]
     (if (map? x)
       (into {} children)
       (assoc x 1 (into {} children))))
   m))

(-> (map-zipper {})
    (zip/append-child [:a 1])
     zip/root)
