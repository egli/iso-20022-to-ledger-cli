(ns iso-20022-to-ledger-cli.core
  (:gen-class)
  (:require [clojure.data.zip.xml :refer [attr text xml-> xml1->]]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.xml :as xml]
            [java-time :as time]
            [clojure.zip :as zip]))

(def default-expense "Expenses:Unknown")
(def default-account "Assets:Postcheckkonto")
(def default-payee "Default Payee")

(def param-mapping
  "Mapping from ISO 20022 to parameters. See [ISO 20022](https://en.wikipedia.org/wiki/ISO_20022)"
  {:amount [:Amt]
   :currency [:Amt (attr :Ccy)]
   :booking-date [:BookgDt :Dt]
   :value-date [:ValDt :Dt]
   :info [:AddtlNtryInf]
   :reference [:AcctSvcrRef]
   :type [:CdtDbtInd]
   })

(defn get-path
  "Get the node for the given `path` in the given `entry`.
  Returns nil if there is no such node"
  [entry path]
  (some->
   (apply xml1-> entry path)))

(defn get-text
  "Get the text for the given `node`. Returns nil if there is no text node"
  [node]
  (some-> node text string/trim))

(defn get-type [type]
  (case type
    "DBIT" :debit
    "CRDT" :credit))

(defn get-date [s]
  (time/local-date s))

(defn read-file
  "Read an export file from VUBIS and return a map with all the data"
  [file]
  (let [root (-> file io/file xml/parse zip/xml-zip)]
    (for [entry (xml-> root :Document :BkToCstmrStmt :Stmt :Ntry)]
      (->> (for [[key path] param-mapping
                 :let [val (let [node (get-path entry path)]
                             (cond
                               (#{:currency} key) node
                               (#{:type} key) (some-> node get-text get-type)
                               (#{:booking-date :value-date} key) (some-> node get-text get-date)
                               :else (some-> node get-text)))]
                 :when (some? val)]
             [key val])
           (into {})))))

(defn render-entry
  [entry]
  (let [{:keys [amount currency booking-date value-date info reference type]} entry]
    (str
     (string/join
      "\n"
      [(str booking-date
            (when (and value-date (not= value-date booking-date))
              (str "=" value-date)))
       (str "    ; " info)
       (str "    " default-expense "            " amount)
       (str "    " default-account)]))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
