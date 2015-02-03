package com.rackspace.feeds.feedscatalog

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.http.HttpServletRequest

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class ResolveHostFilterTest extends Specification {

    @Shared String allfeeds_XmlString = new File('./src/test/resources/test_allfeeds.xml').text

    @Unroll
    def "getTenantIds(): retrieve multiple tenantIds via () with #headerValues "(String headerValues) {
        given:
        HttpServletRequest httpServletRequest = mock(HttpServletRequest)

        def values = new ArrayList<String>(Arrays.asList(headerValues.split(",")));
        when(httpServletRequest.getHeaders("x-tenant-id")).thenReturn(Collections.enumeration(values))

        ResolveHostFilter resolveHostFilter = new ResolveHostFilter()

        when:

        ResolveHostFilter.IdsFromHeader idsFromHeader = resolveHostFilter.getTenantIds(httpServletRequest)

        then:
        idsFromHeader.getTenantId() == "TENANT_ID"
        idsFromHeader.getNastId() == "NASTID_IS_LONGER"

        where:
        [headerValues] << [
                ["TENANT_ID,NASTID_IS_LONGER"],
                ["TENANT_ID,TENANT_ID_2,NASTID_IS_LONGER"],
        ]
    }

    @Unroll
    def "getTenantIds(): retrieve one tenantId via () with #headerValue "(String headerValue) {
        given:
        HttpServletRequest httpServletRequest = mock(HttpServletRequest)

        def values = new ArrayList<String>(Arrays.asList(headerValue));
        when(httpServletRequest.getHeaders("x-tenant-id")).thenReturn(Collections.enumeration(values))

        ResolveHostFilter resolveHostFilter = new ResolveHostFilter()

        when:

        ResolveHostFilter.IdsFromHeader idsFromHeader = resolveHostFilter.getTenantIds(httpServletRequest)

        then:
        idsFromHeader.getTenantId() == headerValue
        idsFromHeader.getNastId() == headerValue

        where:
        [headerValue] << [
                ["TENANT_ID"],
                ["TENANT_ID_2"],
        ]
    }

    @Unroll
    def "resolveHostname(): resolve internal hostname from xml file #hostFilePath"(String hostFilePath) {
        given:
        HttpServletRequest httpServletRequest = mock(HttpServletRequest)

        when(httpServletRequest.getHeader("x-external-loc")).thenReturn(null)

        ResolveHostFilter resolveHostFilter = new ResolveHostFilter()
        resolveHostFilter.setHostFilePath(hostFilePath)

        def oldResponse = allfeeds_XmlString

        when:

        def newResponse = resolveHostFilter.resolveHostname(httpServletRequest, oldResponse)

        then:

        newResponse == oldResponse.replace("http://localhost", "https://internal.atom.vip")

        where:
        [hostFilePath] << [
                ["./src/test/resources/test_feedscatalog.xml"],
        ]
    }

    @Unroll
    def "resolveHostname(): resolve external hostname from xml file #hostFilePath"(String hostFilePath) {
        given:
        HttpServletRequest httpServletRequest = mock(HttpServletRequest)

        when(httpServletRequest.getHeader("x-external-loc")).thenReturn("http://localhost:9090")

        ResolveHostFilter resolveHostFilter = new ResolveHostFilter()
        resolveHostFilter.setHostFilePath(hostFilePath)

        def oldResponse = allfeeds_XmlString

        when:

        def newResponse = resolveHostFilter.resolveHostname(httpServletRequest, oldResponse)

        then:

        newResponse == oldResponse.replace("http://localhost", "https://external.feeds.vip")

        where:
        [hostFilePath] << [
                ["./src/test/resources/test_feedscatalog.xml"],
        ]
    }
}
