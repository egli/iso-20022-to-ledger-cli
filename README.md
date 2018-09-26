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

# License

GPLv3+
