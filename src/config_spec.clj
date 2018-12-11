(ns config-spec
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]))

(defn non-blank-string? [x] (and (string? x) (not (string/blank? x))))

(s/def ::default-payee non-blank-string?)
(s/def ::default-account non-blank-string?)
(s/def ::default-expense non-blank-string?)
(s/def ::default-income non-blank-string?)

(s/def ::regexps (s/*
                  (s/cat :regexp non-blank-string?
                         :account (s/or :key keyword? :str non-blank-string?)
                         :payee non-blank-string?)))

(s/def ::names (s/map-of keyword? non-blank-string?))

(s/def ::config (s/keys :req-un [::regexps
                                 ::names
                                 ::default-payee ::default-account
                                 ::default-expense ::default-income]))

