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
    <xsl:call-template name="getTenantId">
      <xsl:with-param name="uri"
                      select="$requestDoc/httpx:request-information/httpx:uri"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:template name="getTenantId">
    <xsl:param name="uri"/>
    <xsl:analyze-string select="$uri"
                        regex="^/feedscatalog/catalog/([^/?]+)/?">
      <xsl:matching-substring>
        <xsl:value-of select="regex-group(1)"/>
      </xsl:matching-substring>
    </xsl:analyze-string>
  </xsl:template>
  
  <xsl:template match="node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
  </xsl:template>
  
  <xsl:param name="environment" select="document('/etc/feedscatalog/feedscatalog.xml')"/>
  
  <xsl:template xmlns:cfc="http://docs.rackspace.com/cloudfeeds/catalog" 
    match="@region[parent::cfc:endpoint]">
    <xsl:attribute name="region"><xsl:value-of select="$environment/environment/region/text()"/></xsl:attribute>
  </xsl:template>
  
  <xsl:template xmlns:cfc="http://docs.rackspace.com/cloudfeeds/catalog" 
    match="@tenantId[parent::cfc:endpoint]">
    <xsl:attribute name="tenantId"><xsl:value-of select="$tenantId"/></xsl:attribute>
  </xsl:template>
  
  <xsl:template xmlns:cfc="http://docs.rackspace.com/cloudfeeds/catalog" 
    match="@publicURL[parent::cfc:endpoint]">
    <xsl:variable name="newPublicURL"><xsl:value-of select="replace(current(), '\$\{tenantId\}', $tenantId)"/></xsl:variable>
    <xsl:attribute name="publicURL"><xsl:value-of select="replace($newPublicURL, 'http://localhost', $environment/environment/vipURL/text())"/></xsl:attribute>
  </xsl:template>
</xsl:stylesheet>
