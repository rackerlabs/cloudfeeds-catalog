package com.rackspace.feeds.feedscatalog

import com.rackspace.feeds.utils.BaseTest
import spock.lang.Shared

import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

class Wadl2SvcDocTest extends BaseTest {

    @Shared xslt = "./src/main/xsl/wadl2SvcDoc.xsl"

    @Shared String wadlWithNestedResource

    def setupMoreSpec() {
        wadlWithNestedResource = '''
<?xml version="1.0" encoding="UTF-8"?>
<application xmlns="http://wadl.dev.java.net/2009/02"
             xmlns:xs="http://www.w3.org/2001/XMLSchema"
             xmlns:rax="http://docs.rackspace.com/api"
             xmlns:atom="http://www.w3.org/2005/Atom">
    <doc title="My Services"/>
    <grammars>
        <include href="xmlschema/schema1.xsd"/>
        <include href="xmlschema/schema2.xsd"/>
    </grammars>
    <resources base="http://myhost/">
        <!-- Normal Resources -->
        <resource id="service1_id"
                  path="service1/path"
                  type="wadl/child.wadl#Type1 wadl/feed.wadl#TenantAtomFeed"/>
        <resource id="service2_id"
                  path="service2/path"
                  type="wadl/child.wadl#Type2 wadl/feed.wadl#TenantFeedsCatalog"/>
        <resource id="service4_id"
                  path="service4/path"
                  type="wadl/child.wadl#Type4"/>

        <!-- Nested Resources -->
        <resource path="nested/">
            <resource id="nested_service3_id"
                      path="service3/path"
                      type="wadl/child.wadl#Type3 wadl/feed.wadl#TenantAtomFeed"/>
        </resource>
    </resources>
</application>
        '''.trim()

        System.setProperty("javax.xml.transform.TransformerFactory",
                            "net.sf.saxon.TransformerFactoryImpl");
    }

    def "Generate Service Doc for tenanted from wadl with nested resources"() {

        when:
        def transformer = transformerFactory.newTransformer(new StreamSource(new FileReader(xslt)))
        def output = new ByteArrayOutputStream()
        transformer.setParameter("generateTenantId", "true")
        transformer.setParameter("usageSchemaVers", "1.10")
        transformer.transform(new StreamSource(new StringReader(wadlWithNestedResource)), new StreamResult(output))

        def result = output.toString()

        then:
        assert getStringValue(result, "/service/workspace[1]/collection/@href") == "http://myhost/service1/path/\${tenantId}"
        assert getStringValue(result, "/service/workspace[2]/collection/@href") == "http://myhost/service2/path/\${tenantId}"
        assert getStringValue(result, "/service/workspace[3]/collection/@href") == "http://myhost/nested/service3/path/\${tenantId}"

    }

    def "Generate Service Doc for service admins from wadl with nested resources"() {

        when:
        def transformer = transformerFactory.newTransformer(new StreamSource(new FileReader(xslt)))
        def output = new ByteArrayOutputStream()
        transformer.setParameter("generateTenantId", "false")
        transformer.setParameter("usageSchemaVers", "1.20")
        transformer.transform(new StreamSource(new StringReader(wadlWithNestedResource)), new StreamResult(output))

        def result = output.toString()

        then:
        assert getStringValue(result, "/service/workspace[1]/collection/@href") == "http://myhost/service1/path"
        assert getStringValue(result, "/service/workspace[2]/collection/@href") == "http://myhost/service2/path"
        assert getStringValue(result, "/service/workspace[3]/collection/@href") == "http://myhost/service4/path"
        assert getStringValue(result, "/service/workspace[4]/collection/@href") == "http://myhost/nested/service3/path"

    }

    def "Generated Svc Doc should have Prefs Svc endpoint"() {
        when:
        def transformer = transformerFactory.newTransformer(new StreamSource(new FileReader(xslt)))
        def output = new ByteArrayOutputStream()
        transformer.setParameter("generateTenantId", "false")
        transformer.setParameter("usageSchemaVers", "1.20")
        transformer.transform(new StreamSource(new StringReader(wadlWithNestedResource)), new StreamResult(output))

        def result = output.toString()

        then:
        assert getStringValue(result, "/service/workspace[last()]/link[@rel='archive-preferences']/@href") == "\${prefsSvcVipUrl}/archive/\${tenantId}"
    }
}
