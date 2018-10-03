# ISO 20022 to ledger-cli

Convert [ISO 20022](https://en.wikipedia.org/wiki/ISO_20022) to the
[ledger-cli](https://www.ledger-cli.org) plain text accounting format.

Obviously this goes without saying: This software is provided "as is"
without warranty of any kind. If your ledger statements are incorrect
and you believe you found a bug in the converter please file an issue
or even better a pull request.

# Usage

``` shell
lein run iso_20022.xml plain_text.ledger
```
# Requirements

You will need Leiningen to run this code. Install as follows:

``` shell
sudo apt install leiningen
```
# Todo

- [ ] Explore the use of [clj](https://clojure.org/guides/deps_and_cli)
  - an interesting example on using [a shebang on top of the file](https://clojureverse.org/t/deps-edn-workflows/2451)

# References

The conversion is mostly based on classic reverse engineering of sample xml data. When in doubt also some [official implementation guidelines](https://www.six-group.com/interbank-clearing/dam/downloads/de/standardization/iso/swiss-recommendations/implementation-guidelines-camt.pdf) were consulted.

# License

GPLv3+
