# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Converts ISO 20022 camt.053/camt.054 bank statements (XML) to CSV for hledger. The whole converter is one XSLT 1.0 stylesheet run with `xsltproc`; there is no build. The author runs it about once a year.

## Commands

```shell
xsltproc camt-to-csv.xsl statement.xml > statement.csv
hledger import statement.csv --rules camt.rules
xsltproc camt-to-csv.xsl test/sample.xml | diff - test/sample.csv   # the test
```

`test/sample.xml` covers the known edge cases (unsorted entries, missing optional `AcctSvcrRef`/`ValDt`/`TxDtls`, batch entries, counterparty in both name nestings, `DtTm` instead of `Dt`, quotes/commas in text, DBIT closing balance). Extend it and regenerate `test/sample.csv` when changing the stylesheet.

## Design

- Stay on XSLT 1.0 so plain `xsltproc` works; no Saxon/Java.
- Elements are matched with `local-name()` so every camt schema version/namespace works.
- Columns are fixed and every row always has all of them; the old Clojure version wrote map values in hash order and shifted columns when optional elements were missing, which mixed up credits and debits.
- One row per `TxDtls` (with that payment's own `Amt`/`CdtDbtInd`), plus one row per `Ntry` that has no `TxDtls` (e.g. fees). Counterparty name/IBAN come from `RltdPties/Cdtr*` for debits and `RltdPties/Dbtr*` for credits (the other side is the account owner).
- Amounts are signed from `CdtDbtInd` (DBIT = negative), for entries and the closing (`CLBD`) balance alike. The closing balance goes only on the last row, as an hledger balance assertion.
- Account classification is deliberately left to hledger (`if` blocks in `camt.rules`), not done in the XSLT.
