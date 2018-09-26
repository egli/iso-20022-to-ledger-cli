# ISO 20022 to ledger-cli

Convert [ISO 20022](https://en.wikipedia.org/wiki/ISO_20022) to the
[ledger-cli](https://www.ledger-cli.org) plain text accounting format.

Obviously this goes without saying: This software is provided "as is"
without warranty of any kind. If your ledger statements are incorrect
and you believe you found a bug in the converter please file an issue
or even better a pull request.

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
# TODO

The progam should be able to automatically deduce the payee and the expense
from the additional entry info. This of course should be configurable by the
user, or even better use some ML to find the best match.  

# License

GPLv3+
