<?xml version="1.0" encoding="UTF-8"?>
<!-- 
    This XSLT turns our Cloud Feeds WADLs into a Feed Catalog.
-->
<xsl:stylesheet 
         xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
         xmlns:wadl="http://wadl.dev.java.net/2009/02"
         xmlns:xs="http://www.w3.org/2001/XMLSchema"
         xmlns:atom="http://www.w3.org/2005/Atom"
         xmlns:ns="urn:namespace"
         xmlns="http://www.w3.org/2007/app"
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
        <service xmlns="http://www.w3.org/2007/app"
                 xmlns:atom="http://www.w3.org/2005/Atom">
            <!-- take the resources base URL -->
            <xsl:apply-templates select="wadl:resource" mode="expand">
                <xsl:with-param name="base" select="@base"/>
            </xsl:apply-templates>
            <xsl:comment>
                <xsl:text> Generated from schema version </xsl:text>
                <xsl:value-of select="$usageSchemaVers"/>
                <xsl:text> </xsl:text>
            </xsl:comment>
            <xsl:text>&#x0a;</xsl:text>
        </service>
    </xsl:template>
    
    <xsl:template match="wadl:resource[@id != 'buildinfo' and @id != 'logtest' and @id != 'evict-me'
                         and not(contains(@id, 'feedscatalog')) and not(matches(@path,'.*test[0-9]*/events'))
                         and not(matches(@path,'\{usagetestid\}/events')) ]" mode="expand">
        <xsl:param name="base"></xsl:param>
        <xsl:variable name="path" select="@path"/>
        <xsl:variable name="isTenantFeed" select="contains( @type, 'wadl/feed.wadl#TenantAtomFeed') or contains( @type, 'TenantFeedsCatalog')"/>

        <xsl:variable name="isFeedUsingNastId" select="matches( @id, 'files_events') or matches( @id, 'files_usagesummary_events')"/>

        <xsl:variable name="parent_path">
            <xsl:choose>
                <xsl:when test="parent::wadl:resource/@path"><xsl:value-of select="parent::wadl:resource/@path"/></xsl:when>
                <xsl:otherwise/>                
            </xsl:choose>    
        </xsl:variable>
        <xsl:if test="($generateTenantId = 'true' and $isTenantFeed) or $generateTenantId = 'false' ">
            <xsl:element name="workspace">
                <xsl:element name="atom:title">
                    <xsl:value-of select="@id"/>
                </xsl:element>
                <xsl:element name="collection">
                    <xsl:attribute name="href"></xsl:attribute>
                    <xsl:choose>
                        <xsl:when test="$generateTenantId = 'true'">
                            <!-- add test here for event = files -->
                            <xsl:choose>
                                <xsl:when test="$isFeedUsingNastId">
                                    <xsl:attribute name="href"><xsl:value-of select="concat($base,$parent_path,$path,'/${nastId}')"/></xsl:attribute>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:attribute name="href"><xsl:value-of select="concat($base,$parent_path,$path,'/${tenantId}')"/></xsl:attribute>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:attribute name="href"><xsl:value-of select="concat($base,$parent_path,$path)"/></xsl:attribute>
                        </xsl:otherwise>
                    </xsl:choose>
                    <xsl:element name="atom:title">
                        <xsl:value-of select="@id"/>
                    </xsl:element>
                </xsl:element>
            </xsl:element>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>
