<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:httpx="http://openrepose.org/repose/httpx/v1.0">

    <xsl:param name="tenantId" />
    <xsl:param name="nastId" />
    <xsl:param name="externalLoc" />

    <xsl:output method="xml" indent="yes"/>

    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

    <xsl:param name="environment">
        <xsl:choose>
            <xsl:when test="doc-available('./src/test/resources/test_feedscatalog.xml')">
                <xsl:copy-of select="document('./src/test/resources/test_feedscatalog.xml')"/>
            </xsl:when>
            <xsl:otherwise><xsl:copy-of select="document('/etc/feedscatalog/feedscatalog.xml')"/></xsl:otherwise>
        </xsl:choose>
    </xsl:param>

    <xsl:template xmlns:app="http://www.w3.org/2007/app"
                  match="/app:service/app:workspace/app:collection/@href">

        <xsl:variable name="vipHostname">
            <xsl:choose>
                <xsl:when test=" $externalLoc != '' ">
                    <xsl:value-of select="$environment/environment/externalVipURL/text()"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$environment/environment/vipURL/text()"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:variable name="urlWithNastId">
            <xsl:value-of select="replace(current(), '\$\{nastId\}', $nastId)"/>
        </xsl:variable>

        <xsl:variable name="urlWithTenantId">
            <xsl:value-of select="replace($urlWithNastId, '\$\{tenantId\}', $tenantId)"/>
        </xsl:variable>

        <xsl:variable name="newUrl">
            <xsl:value-of select="replace($urlWithTenantId, 'http://localhost', $vipHostname)"/>
        </xsl:variable>

        <xsl:attribute name="href"><xsl:value-of select="$newUrl"/></xsl:attribute>

    </xsl:template>

    <xsl:template xmlns:app="http://www.w3.org/2007/app" 
                  xmlns:atom="http://www.w3.org/2005/Atom"
                  match="/app:service/app:workspace/atom:link[@rel='archive-preferences']/@href">

        <xsl:variable name="prefsSvcVip">
            <xsl:value-of select="$environment/environment/prefsSvcVipURL/text()"/>
        </xsl:variable>

        <xsl:variable name="urlWithPrefsSvc">
            <xsl:value-of select="replace(current(), '\$\{prefsSvcVipUrl\}', $prefsSvcVip)"/>
        </xsl:variable>

        <xsl:variable name="urlWithTenantId">
            <xsl:value-of select="replace($urlWithPrefsSvc, '\$\{tenantId\}', $tenantId)"/>
        </xsl:variable>

        <xsl:attribute name="href"><xsl:value-of select="$urlWithTenantId"/></xsl:attribute>

    </xsl:template>

</xsl:stylesheet>
