(ns iso-20022-to-ledger-cli.core
  (:gen-class)
  (:require [clojure.data.zip.xml :refer [attr text xml-> xml1->]]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.xml :as xml]
            [java-time :as time]
            [clojure.zip :as zip]))

(def default-expense "Expenses:Unknown")
(def default-income "Income:Unknown")
(def default-account "Assets:Unknown")
(def default-payee "Unknown Payee")

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

(defn extract-statements [root]
  (let [statement (xml1-> root :Document :BkToCstmrStmt :Stmt)]
    {:id (xml1-> statement :Id text)
     :from (some-> (xml1-> statement :FrToDt :FrDtTm text) time/local-date-time)
     :to (some-> (xml1-> statement :FrToDt :ToDtTm text) time/local-date-time)
     :iban (xml1-> statement :Acct :Id :IBAN text)
     :account (xml1-> statement :Acct :Ownr :Nm text)}))

(defn extract-balance [root]
  (->>
   (for [balance (xml-> root :Document :BkToCstmrStmt :Stmt :Bal)]
     {:ammount (xml1-> balance :Amt text)
      :currency (xml1-> balance :Amt (attr :Ccy))
      :date (xml1-> balance :Dt :Dt text get-date)})
   (sort-by :date)))

(defn extract-entries [root]
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
         (into {}))))

(defn read-file
  "Read an iso20022 file and return a map with all the data"
  [file]
  (let [root (-> file io/file xml/parse zip/xml-zip)
        statements (extract-statements root)
        balance (extract-balance root)
        entries (extract-entries root)]
    (merge statements {:balance balance :entries entries})))

(defn render-statements [{:keys [id from to iban account]}]
  (format
   (string/join
    \newline
    [";; Id: %s"
     ";; From: %s"
     ";; To: %s"
     ";; IBAN: %s"
     ";; Account: %s"])
   id from to iban account))

(defn render-balance [{:keys [ammount currency date]}]
  (format
   (string/join
    \newline
    ["" ; add a newline
     ";; Balance"
     ";; Date %s"
     ";; Amount: %s"
     ";; Currency %s"])
   ammount currency date))

(defn target-account [{:keys [type] :as entry}]
  (if (= type :debit)  default-expense default-account))

(defn source-account [{:keys [type] :as entry}]
  (if (= type :debit) default-account default-income))

(defn render-entry
  [entry]
  (let [{:keys [amount currency booking-date value-date info reference type]} entry]
    (str
     (string/join
      "\n"
      ["" ; add an empty line
       (str booking-date
            (when (and value-date (not= value-date booking-date))
              (str "=" value-date))
            " "
            default-payee)
       (str "    ; " info)
       (str "    " (target-account entry) "            " amount)
       (str "    " (source-account entry))]))))

(defn render [{:keys [balance entries] :as data}]
  (string/join
   \newline
   (concat (map render-statements [data])
           (map render-balance balance)
           (map render-entry entries))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
