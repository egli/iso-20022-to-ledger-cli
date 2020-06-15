(ns iso-to-csv
  (:require [clojure.data.csv :as csv]
            [iso-to-ledger :as ledger]))

(defn- add-balances
  [entries initial-balance final-balance]
  (let [balances (concat [initial-balance] (repeat (- (count entries) 2) nil) [final-balance])]
    (map (fn [entry bal] (assoc entry :balance bal)) entries balances)))

(defn -main
  "Convert [ISO 20022](https://en.wikipedia.org/wiki/ISO_20022) to CSV"
  [& args]
  (doseq [file args]
    (let [data (-> file ledger/read-file)
          [initial-balance final-balance] map :amount (:balance data)
          entries (add-balances (:entries data) initial-balance final-balance)
          header (->> entries first keys (map name))]
      (csv/write-csv *out* (conj (->> entries (map vals)) header)))))
