<?xml version="1.0" encoding="UTF-8"?>
<!-- Convert an ISO 20022 camt.053/camt.054 bank statement to CSV for hledger.
     Usage: xsltproc camt-to-csv.xsl statement.xml > statement.csv
     Elements are matched by local-name() so any camt schema version works.
     One row per payment (TxDtls), or per entry if it has no payment details. -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="text" encoding="UTF-8"/>

  <xsl:variable name="q">"</xsl:variable>
  <xsl:variable name="apos">'</xsl:variable>
  <!-- ponytail: one closing balance per file; group by Stmt if a file ever holds several accounts -->
  <xsl:variable name="closing"
    select="(//*[local-name()='Bal'][*[local-name()='Tp']//*[local-name()='Cd']='CLBD'])[last()]"/>

  <xsl:template match="/">
    <xsl:text>date,value-date,amount,currency,reference,description,counterparty,iban,message,balance&#10;</xsl:text>
    <!-- every payment, plus entries that have no payment details (e.g. fees) -->
    <xsl:for-each select="//*[local-name()='TxDtls']
                        | //*[local-name()='Ntry'][not(.//*[local-name()='TxDtls'])]">
      <xsl:sort select="ancestor-or-self::*[local-name()='Ntry']/*[local-name()='BookgDt']/*"/>
      <xsl:variable name="ntry" select="ancestor-or-self::*[local-name()='Ntry']"/>
      <!-- the row's TxDtls: itself in a batch, else the entry's only one (may be empty) -->
      <xsl:variable name="tx" select="descendant-or-self::*[local-name()='TxDtls']"/>
      <!-- the other party is the creditor when money goes out, the debtor when it comes in -->
      <xsl:variable name="other">
        <xsl:choose>
          <xsl:when test="*[local-name()='CdtDbtInd'] = 'DBIT'">Cdtr</xsl:when>
          <xsl:otherwise>Dbtr</xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:variable name="pties" select="$tx/*[local-name()='RltdPties']"/>

      <!-- BookgDt/ValDt hold either Dt or DtTm; the first 10 chars are the date -->
      <xsl:value-of select="concat(
        substring($ntry/*[local-name()='BookgDt']/*, 1, 10), ',',
        substring($ntry/*[local-name()='ValDt']/*, 1, 10), ',')"/>
      <xsl:apply-templates select="." mode="amount"/>
      <xsl:value-of select="concat(',',
        *[local-name()='Amt']/@Ccy, ',',
        $ntry/*[local-name()='AcctSvcrRef'], ',')"/>
      <xsl:apply-templates select="$ntry/*[local-name()='AddtlNtryInf']" mode="csv"/>
      <xsl:text>,</xsl:text>
      <!-- Nm sits directly under Cdtr/Dbtr in older versions, under Cdtr/Pty/Dbtr/Pty in newer ones -->
      <xsl:apply-templates select="($pties/*[local-name()=$other]//*[local-name()='Nm'])[1]" mode="csv"/>
      <xsl:value-of select="concat(',',
        $pties/*[local-name()=concat($other, 'Acct')]/*[local-name()='Id']/*[local-name()='IBAN'], ',')"/>
      <xsl:apply-templates select="$tx/*[local-name()='RmtInf']/*[local-name()='Ustrd'][1]" mode="csv"/>
      <xsl:text>,</xsl:text>
      <xsl:if test="position() = last()">
        <xsl:apply-templates select="$closing" mode="amount"/>
      </xsl:if>
      <xsl:text>&#10;</xsl:text>
    </xsl:for-each>
  </xsl:template>

  <!-- Ntry, TxDtls and Bal all carry an unsigned Amt plus a CdtDbtInd -->
  <xsl:template match="*" mode="amount">
    <xsl:if test="*[local-name()='CdtDbtInd'] = 'DBIT'">-</xsl:if>
    <xsl:value-of select="*[local-name()='Amt']"/>
  </xsl:template>

  <!-- free text as a quoted CSV field; double quotes become single quotes -->
  <xsl:template match="*" mode="csv">
    <xsl:value-of select="concat($q, translate(normalize-space(.), $q, $apos), $q)"/>
  </xsl:template>
</xsl:stylesheet>
