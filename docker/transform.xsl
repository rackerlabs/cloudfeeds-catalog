<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    
        <xsl:template match="node()|@*">
            <xsl:copy>
                <xsl:apply-templates select="node()|@*"/>
            </xsl:copy>
        </xsl:template>
    
        <xsl:template match="/Server/Service/Engine/Host/Valve">
            <xsl:copy>
                <xsl:apply-templates select="node()|@*"/>
                <xsl:attribute name="maxDays">
                    <xsl:value-of select="'3'"/>
                </xsl:attribute>
            </xsl:copy>
        </xsl:template>
    
</xsl:stylesheet>
