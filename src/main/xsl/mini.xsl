<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:httpx="http://openrepose.org/repose/httpx/v1.0">

  <xsl:output method="xml" indent="yes"/>
  <xsl:variable name="tenantId" select="concat('123','456')"></xsl:variable>
  <xsl:variable name="region" select="concat('DF','W')"></xsl:variable>
  <xsl:strip-space elements="*"/>
  
  <xsl:template match="node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template xmlns:cfc="http://docs.rackspace.com/cloudfeeds/catalog" 
                    match="@region[parent::cfc:endpoint]">
    <xsl:attribute name="region"><xsl:value-of select="$region"/></xsl:attribute>
  </xsl:template>

  <xsl:template xmlns:cfc="http://docs.rackspace.com/cloudfeeds/catalog" 
                    match="@tenantId[parent::cfc:endpoint]">
      <xsl:attribute name="tenantId"><xsl:value-of select="$tenantId"/></xsl:attribute>
  </xsl:template>
  
  <xsl:template xmlns:cfc="http://docs.rackspace.com/cloudfeeds/catalog" 
                     match="@publicURL[parent::cfc:endpoint]">
    <xsl:attribute name="publicURL"><xsl:value-of select="replace(current(), '\$\{tenantId\}', $tenantId)"/></xsl:attribute>
  </xsl:template>
    
</xsl:stylesheet>
