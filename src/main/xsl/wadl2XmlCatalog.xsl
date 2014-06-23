<?xml version="1.0" encoding="UTF-8"?>
<!-- 
    This XSLT turns our Cloud Feeds WADLs into a Feed Catalog.
-->
<xsl:stylesheet 
         xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
         xmlns:wadl="http://wadl.dev.java.net/2009/02"
         xmlns:xs="http://www.w3.org/2001/XMLSchema"
         xmlns:ns="urn:namespace"
         xmlns="http://docs.rackspace.com/cloudfeeds/catalog"
         exclude-result-prefixes="xsl wadl xs ns"
         version="2.0">
    <xsl:output 
        method="xml" 
        encoding="UTF-8" 
        indent="yes"/>   
    <xsl:param name="generateTenantId" as="xs:string"/>
    <xsl:param name="usageSchemaVers" as="xs:string"/>
    <xsl:strip-space elements="*"/>
    <xsl:template match="/wadl:application/wadl:resources">
        <feedCatalog>
            <version><xsl:value-of select="$usageSchemaVers"/></version>
            <!-- take the resources base URL -->
            <xsl:apply-templates select="wadl:resource/*" mode="expand">
                <xsl:with-param name="base" select="@base"/>
            </xsl:apply-templates>
        </feedCatalog>
    </xsl:template>
    
    <xsl:template match="wadl:resource[@id != 'buildinfo' and @id != 'logtest' and @id != 'evict-me' and not(contains(@id, 'feedscatalog')) and not(matches(@path,'.*test[0-9]*/events')) ]" mode="expand">
        <xsl:param name="base"></xsl:param>
        <xsl:variable name="path" select="@path"/>
        <xsl:variable name="parent_path">
            <xsl:choose>
                <xsl:when test="parent::wadl:resource/@path"><xsl:value-of select="concat(parent::wadl:resource/@path,'/')"/></xsl:when>
                <xsl:otherwise/>                
            </xsl:choose>    
        </xsl:variable>
        <xsl:element name="feed">
            <xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute>
            <xsl:element name="endpoint">
                <xsl:attribute name="region">${region}</xsl:attribute>
                <xsl:choose>
                    <xsl:when test="$generateTenantId = 'true'">
                        <xsl:attribute name="tenantId">${tenantId}</xsl:attribute>
                        <xsl:attribute name="publicURL"><xsl:value-of select="concat($base,$parent_path,$path,'/${tenantId}')"/></xsl:attribute>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:attribute name="publicURL"><xsl:value-of select="concat($base,$parent_path,$path)"/></xsl:attribute>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:element>
        </xsl:element>
    </xsl:template>

</xsl:stylesheet>
