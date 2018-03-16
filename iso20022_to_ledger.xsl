<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet
    version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:func="http://functions"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:iso="urn:iso:std:iso:20022:tech:xsd:camt.053.001.04"
    extension-element-prefixes="func">

  <xsl:output method="text" encoding="utf-8"/>
  <xsl:strip-space elements="*"/>

  <xsl:param name="default-account">Assets:Postcheckkonto</xsl:param>
  <xsl:param name="default-payee">Default Payee</xsl:param>
  <xsl:param name="default-expense">Expenses:Unknown</xsl:param>

  <xsl:function name="func:amount" as="xs:string">
    <xsl:param name="context" as="node()"/>
    <xsl:sequence select="concat($context/text(), ' ', $context/@Ccy)"/>
  </xsl:function>

  <xsl:function name="func:date" as="xs:string">
    <xsl:param name="context" as="node()"/>
    <xsl:variable name="booking-date" select="$context/iso:BookgDt/iso:Dt/text()"/>
    <xsl:variable name="value-date" select="$context/iso:ValDt/iso:Dt/text()"/>
    <xsl:sequence select="if ($value-date = $booking-date) then $value-date else concat($value-date,'=',$booking-date)"/>
  </xsl:function>

  <xsl:template match="iso:Document">
    <xsl:apply-templates select="iso:BkToCstmrStmt"/>
  </xsl:template>

  <xsl:template match="iso:BkToCstmrStmt">
    <xsl:apply-templates select="iso:Stmt"/>
  </xsl:template>

  <xsl:template match="iso:Stmt">
    <xsl:value-of select="concat('# Import: ', iso:Id, '&#10;')"/>
    <xsl:value-of
      select="concat('# Date: ', iso:FrToDt/iso:FrDtTm, ' - ', iso:FrToDt/iso:ToDtTm, '&#10;&#10;')"/>
    <xsl:apply-templates select="iso:Acct"/>
    <xsl:text># Balance:&#10;</xsl:text>
    <xsl:apply-templates select="iso:Bal"/>
    <xsl:text>&#10;</xsl:text>
    <xsl:apply-templates select="iso:Ntry"/>
  </xsl:template>

  <xsl:template match="iso:Acct">
    <xsl:text># Account:&#10;</xsl:text>
    <xsl:value-of select="concat('#   IBAN: ', iso:Id/iso:IBAN,'&#10;')"/>
    <xsl:value-of select="concat('#   Owner: ', iso:Ownr/iso:Nm,'&#10;')"/>
  </xsl:template>

  <xsl:template match="iso:Bal">
    <xsl:value-of select="concat('#   ', iso:Dt/iso:Dt,': ', func:amount(iso:Amt), '&#10;')"/>
  </xsl:template>

  <xsl:template match="iso:Ntry">
    <xsl:value-of select="concat(func:date(.),' * ', $default-payee, '&#10;')"/>
    <xsl:value-of select="concat('    ; ', iso:AddtlNtryInf, '&#10;')"/>
    <xsl:value-of
      select="concat('    ', $default-expense, '                 ', func:amount(iso:Amt), '&#10;')"/>
    <xsl:value-of select="concat('    ', $default-account, '&#10;&#10;')"/>
  </xsl:template>

  <xsl:template match="*">
    <xsl:message terminate="no"> WARNING: Unmatched element: <xsl:value-of select="name()"/>
    </xsl:message>
    <xsl:apply-templates/>
  </xsl:template>

</xsl:stylesheet>
