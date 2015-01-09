package com.rackspace.feeds.feedscatalog

import com.rackspace.feeds.utils.BaseTest
import spock.lang.Shared

import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

class feedscatalogResolveTest extends BaseTest {

    @Shared xslt = "./src/main/xsl/feedscatalog-resolve.xsl"
    @Shared String allfeeds_XmlString = new File('./src/test/resources/test_allfeeds.xml').text
    @Shared String allfeeds_observer_XmlString = new File('./src/test/resources/test_allfeeds_observer.xml').text

    def setupMoreSpec() {
        System.setProperty("javax.xml.transform.TransformerFactory",
                            "net.sf.saxon.TransformerFactoryImpl");
    }

    def "Generate all feeds catalog"() {
        when:
        def transformer = transformerFactory.newTransformer(new StreamSource(new FileReader(xslt)))
        def output = new ByteArrayOutputStream()
        transformer.setParameter("input-headers-uri", "./src/test/resources/repose-input-headers-uri.xml")
        transformer.setParameter("input-query-uri", "./src/test/resources/repose-input-query-uri.xml")
        transformer.setParameter("input-request-uri", "./src/test/resources/repose-input-request-uri.xml")

        transformer.transform(new StreamSource(new StringReader(allfeeds_XmlString)), new StreamResult(output))

        def result = output.toString()

        then:
        assert getStringValue(result, "/service/workspace[1]/title") == "backup_events"
        assert getStringValue(result, "/service/workspace[1]/collection/@href").contains("/backup/events")

        assert getStringValue(result, "/service/workspace[2]/title") == "bigdata_events"
        assert getStringValue(result, "/service/workspace[2]/collection/@href").contains("/bigdata/events")

        assert getStringValue(result, "/service/workspace[3]/title") == "files_events"
        assert getStringValue(result, "/service/workspace[3]/collection/@href").contains("/files/events")

        assert getStringValue(result, "/service/workspace[4]/title") == "files_usagesummary_events"
        assert getStringValue(result, "/service/workspace[4]/collection/@href").contains("/usagesummary/files/events")
    }

    def "Generate feeds catalog with tenantId using feedscatalog-resolve.xsl"() {
        when:
        def transformer = transformerFactory.newTransformer(new StreamSource(new FileReader(xslt)))
        def output = new ByteArrayOutputStream()
        transformer.setParameter("input-headers-uri", "./src/test/resources/repose-input-headers-uri.xml")
        transformer.setParameter("input-query-uri", "./src/test/resources/repose-input-query-uri.xml")
        transformer.setParameter("input-request-uri", "./src/test/resources/repose-input-request-uri.xml")

        transformer.transform(new StreamSource(new StringReader(allfeeds_observer_XmlString)), new StreamResult(output))

        def result = output.toString()

        then:
        assert getStringValue(result, "/service/workspace[1]/title") == "backup_events"
        assert getStringValue(result, "/service/workspace[1]/collection/@href").contains("/backup/events/1234567")

        assert getStringValue(result, "/service/workspace[2]/title") == "bigdata_events"
        assert getStringValue(result, "/service/workspace[2]/collection/@href").contains("/bigdata/events/1234567")

        assert getStringValue(result, "/service/workspace[3]/title") == "files_events"
        assert getStringValue(result, "/service/workspace[3]/collection/@href").contains("/files/events/MossoID_aaa1-bbb2-ccc3-ddd4-eee5")

        assert getStringValue(result, "/service/workspace[4]/title") == "files_usagesummary_events"
        assert getStringValue(result, "/service/workspace[4]/collection/@href").contains("/usagesummary/files/events/MossoID_aaa1-bbb2-ccc3-ddd4-eee5")
    }
}
