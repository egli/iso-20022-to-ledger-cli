# ISO 20022 to ledger-cli

Convert [ISO 20022](https://en.wikipedia.org/wiki/ISO_20022) to the [ledger-cli](https://www.ledger-cli.org) plain text accounting format.

# Usage

``` shell
saxonb-xslt iso_20022.xml iso20022_to_ledger.xsl > plain_text.ledger
```
# Requirements

You need an XSLT processor that supports XSLT 2.0, such as Saxon for
example. Install as follows:

``` shell
sudo apt install libsaxonb-java
```

# License

GPLv3+
