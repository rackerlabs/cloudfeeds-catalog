package com.rackspace.feeds.feedscatalog

import com.rackspace.feeds.utils.BaseTest
import spock.lang.Shared

import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

class feedscatalogResolveTest extends BaseTest {

    @Shared xslt = "./src/main/resources/feedscatalog-resolve-host.xsl"
    @Shared String allfeeds_XmlString = new File('./src/test/resources/test_allfeeds.xml').text
    @Shared String allfeeds_observer_XmlString = new File('./src/test/resources/test_allfeeds_observer.xml').text

    @Shared String TENANT_ID= "1234567"
    @Shared String NAST_ID= "MossoID_aaa1-bbb2-ccc3-ddd4-eee5"

    def setupMoreSpec() {
        System.setProperty("javax.xml.transform.TransformerFactory",
                "net.sf.saxon.TransformerFactoryImpl");
    }

    def "Generate all feeds catalog for internal node"() {
        when:
        def transformer = transformerFactory.newTransformer(new StreamSource(new FileReader(xslt)))
        def output = new ByteArrayOutputStream()

        transformer.transform(new StreamSource(new StringReader(allfeeds_XmlString)), new StreamResult(output))

        def result = output.toString()

        then:
        assert getStringValue(result, "/service/workspace[1]/title") == "backup_events"
        assert getStringValue(result, "/service/workspace[1]/collection/@href") == "https://internal.atom.vip/backup/events"

        assert getStringValue(result, "/service/workspace[2]/title") == "bigdata_events"
        assert getStringValue(result, "/service/workspace[2]/collection/@href") == "https://internal.atom.vip/bigdata/events"

        assert getStringValue(result, "/service/workspace[3]/title") == "files_events"
        assert getStringValue(result, "/service/workspace[3]/collection/@href") == "https://internal.atom.vip/files/events"

        assert getStringValue(result, "/service/workspace[4]/title") == "files_usagesummary_events"
        assert getStringValue(result, "/service/workspace[4]/collection/@href") == "https://internal.atom.vip/usagesummary/files/events"
    }

    def "Generate all feeds catalog for external node"() {
        when:
        def transformer = transformerFactory.newTransformer(new StreamSource(new FileReader(xslt)))
        def output = new ByteArrayOutputStream()
        transformer.setParameter("externalLoc", "localhost:9090")

        transformer.transform(new StreamSource(new StringReader(allfeeds_XmlString)), new StreamResult(output))

        def result = output.toString()

        then:
        assert getStringValue(result, "/service/workspace[1]/title") == "backup_events"
        assert getStringValue(result, "/service/workspace[1]/collection/@href") == "https://external.feeds.vip/backup/events"

        assert getStringValue(result, "/service/workspace[2]/title") == "bigdata_events"
        assert getStringValue(result, "/service/workspace[2]/collection/@href") == "https://external.feeds.vip/bigdata/events"

        assert getStringValue(result, "/service/workspace[3]/title") == "files_events"
        assert getStringValue(result, "/service/workspace[3]/collection/@href") == "https://external.feeds.vip/files/events"

        assert getStringValue(result, "/service/workspace[4]/title") == "files_usagesummary_events"
        assert getStringValue(result, "/service/workspace[4]/collection/@href") == "https://external.feeds.vip/usagesummary/files/events"
    }

    def "Generate feeds catalog with tenantId for internal node"() {
        when:
        def transformer = transformerFactory.newTransformer(new StreamSource(new FileReader(xslt)))
        def output = new ByteArrayOutputStream()

        transformer.setParameter("tenantId", TENANT_ID)
        transformer.setParameter("nastId", NAST_ID)

        transformer.transform(new StreamSource(new StringReader(allfeeds_observer_XmlString)), new StreamResult(output))

        def result = output.toString()

        then:
        assert getStringValue(result, "/service/workspace[1]/title") == "backup_events"
        assert getStringValue(result, "/service/workspace[1]/collection/@href") == "https://internal.atom.vip/backup/events/" + TENANT_ID

        assert getStringValue(result, "/service/workspace[2]/title") == "bigdata_events"
        assert getStringValue(result, "/service/workspace[2]/collection/@href") == "https://internal.atom.vip/bigdata/events/" + TENANT_ID

        assert getStringValue(result, "/service/workspace[3]/title") == "files_events"
        assert getStringValue(result, "/service/workspace[3]/collection/@href") == "https://internal.atom.vip/files/events/" + NAST_ID

        assert getStringValue(result, "/service/workspace[4]/title") == "files_usagesummary_events"
        assert getStringValue(result, "/service/workspace[4]/collection/@href") == "https://internal.atom.vip/usagesummary/files/events/" + NAST_ID
    }

    def "Generate feeds catalog with tenantId for external node"() {
        when:
        def transformer = transformerFactory.newTransformer(new StreamSource(new FileReader(xslt)))
        def output = new ByteArrayOutputStream()

        transformer.setParameter("tenantId", TENANT_ID)
        transformer.setParameter("nastId", NAST_ID)
        transformer.setParameter("externalLoc", "localhost:9090")

        transformer.transform(new StreamSource(new StringReader(allfeeds_observer_XmlString)), new StreamResult(output))

        def result = output.toString()

        then:
        assert getStringValue(result, "/service/workspace[1]/title") == "backup_events"
        assert getStringValue(result, "/service/workspace[1]/collection/@href") == "https://external.feeds.vip/backup/events/" + TENANT_ID

        assert getStringValue(result, "/service/workspace[2]/title") == "bigdata_events"
        assert getStringValue(result, "/service/workspace[2]/collection/@href") == "https://external.feeds.vip/bigdata/events/" + TENANT_ID

        assert getStringValue(result, "/service/workspace[3]/title") == "files_events"
        assert getStringValue(result, "/service/workspace[3]/collection/@href") == "https://external.feeds.vip/files/events/" + NAST_ID

        assert getStringValue(result, "/service/workspace[4]/title") == "files_usagesummary_events"
        assert getStringValue(result, "/service/workspace[4]/collection/@href") == "https://external.feeds.vip/usagesummary/files/events/" + NAST_ID
    }
}
