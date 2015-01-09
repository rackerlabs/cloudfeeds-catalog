<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:httpx="http://openrepose.org/repose/httpx/v1.0">

  <xsl:param name="input-headers-uri" />
  <xsl:param name="input-query-uri" />
  <xsl:param name="input-request-uri" />
  <xsl:param name="output-headers-uri" />
  <xsl:param name="output-query-uri" />
  <xsl:param name="output-request-uri" />

  <xsl:variable name="headerDoc" select="doc($input-headers-uri)"/>
  <xsl:variable name="queryDoc" select="doc($input-query-uri)"/>
  <xsl:variable name="requestDoc" select="doc($input-request-uri)"/>

  <xsl:output method="xml" indent="yes"/>

  <xsl:variable name="tenantId">
    <xsl:for-each select="$headerDoc/httpx:headers/httpx:request/httpx:header[@name='x-tenant-id']">
      <xsl:sort select="string-length(@value)" order="ascending" data-type="number" />
      <xsl:if test="position()=1">
        <xsl:value-of select="@value" />
      </xsl:if>
    </xsl:for-each>
  </xsl:variable>

  <xsl:variable name="nastId">
    <xsl:for-each select="$headerDoc/httpx:headers/httpx:request/httpx:header[@name='x-tenant-id']">
      <xsl:sort select="string-length(@value)" order="descending" data-type="number" />
      <xsl:if test="position()=1">
        <xsl:value-of select="@value" />
      </xsl:if>
    </xsl:for-each>
  </xsl:variable>

  <xsl:template match="node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
  </xsl:template>

  <xsl:param name="environment" select="document('/etc/feedscatalog/feedscatalog.xml')"/>

  <xsl:template xmlns:app="http://www.w3.org/2007/app"
    match="/app:service/app:workspace/app:collection/@href">
    <xsl:variable name="urlWithNastId"><xsl:value-of select="replace(current(), '\$\{nastId\}', $nastId)"/></xsl:variable>
    <xsl:variable name="urlWithTenantId"><xsl:value-of select="replace($urlWithNastId, '\$\{tenantId\}', $tenantId)"/></xsl:variable>
    <xsl:variable name="newUrl"><xsl:value-of select="replace($urlWithTenantId, 'http://localhost', $environment/environment/vipURL/text())"/></xsl:variable>
    <xsl:attribute name="href"><xsl:value-of select="$newUrl"/></xsl:attribute>
  </xsl:template>
</xsl:stylesheet>
