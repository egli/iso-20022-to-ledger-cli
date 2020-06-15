(ns iso-to-csv
  (:require [clojure.data.csv :as csv]
            [iso-to-ledger :as ledger]))

(defn- balances
  [data]
  (let [entries (:entries data)
        [initial-balance final-balance] (map :amount (:balance data))]
    (concat [initial-balance] (repeat (- (count entries) 2) nil) [final-balance])))

(defn- credit? [{type :type}]
  (= type :credit))

(defn- add-amount-in
  [entry]
  (assoc entry :amount-in (when (credit? entry) (:amount entry))))

(defn- add-amount-out
  [entry]
  (assoc entry :amount-out (when-not (credit? entry) (:amount entry))))

(defn -main
  "Convert [ISO 20022](https://en.wikipedia.org/wiki/ISO_20022) to CSV"
  [& args]
  (doseq [file args]
    (let [data (-> file ledger/read-file)
          balances (balances data)
          entries (->>
                   (:entries data)
                   (map add-amount-in)
                   (map add-amount-out)
                   (map (fn [v m] (assoc m :balance v)) balances))
          header (->> entries first keys (map name))]
      (csv/write-csv *out* (conj (->> entries (map vals)) header)))))
