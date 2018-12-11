(ns iso-to-ledger
  (:require [clojure.data.zip.xml :refer [attr text xml-> xml1->]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [java-time :as time]))

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
                             (#{:amount} key) (some-> node get-text bigdec)
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

(defn get-account [{:keys [info]} {:keys [regexps names] :as config} not-found]
  (let [account (some
                 (fn [[regexp account _]]
                   (when (re-find (re-pattern regexp) info) account))
                 (partition 3 regexps))]
    (cond
      (keyword? account) (get names account (name account))
      (string? account) account
      :else not-found)))

(defn get-target-account [{:keys [type] :as entry} {:keys [default-account default-expense] :as config}]
  (if (= type :debit) (get-account entry config default-expense) default-account))

(defn get-source-account [{:keys [type] :as entry} {:keys [default-account default-income] :as config}]
  (if (= type :debit) default-account default-income))

(defn get-payee [{:keys [info] :as entry} {:keys [regexps names default-payee] :as config}]
  (let [payee (some
               (fn [[regexp _ payee]]
                 (when (re-find (re-pattern regexp) info) payee))
               (partition 3 regexps))]
    (cond
      (keyword? payee) (get names payee (name payee))
      (string? payee) payee
      :else default-payee)))

(defn render-entry
  [{:keys [amount currency booking-date value-date info reference] :as entry} config]
  (str
   (string/join
    "\n"
    ["" ; add an empty line
     (str booking-date
          (when (and value-date (not= value-date booking-date))
            (str "=" value-date))
          " "
          (get-payee entry config))
     (str "    ; " info)
     (str "    " (get-target-account entry config) "            " amount)
     (str "    " (get-source-account entry config))])))

(defn render [{:keys [balance entries] :as data} config]
  (string/join
   \newline
   (concat (map render-statements [data])
           (map render-balance balance)
           (map #(render-entry % config) entries))))

(defn -main
  "Convert [ISO 20022](https://en.wikipedia.org/wiki/ISO_20022) to [ledger-cli plain text accounting format](https://www.ledger-cli.org) "
  [& args]
  (let [config (with-open [r (io/reader "config.edn")]
                 (edn/read (java.io.PushbackReader. r)))
        data (read-file (first args))
        txt (render data config)]
    (println txt)))
