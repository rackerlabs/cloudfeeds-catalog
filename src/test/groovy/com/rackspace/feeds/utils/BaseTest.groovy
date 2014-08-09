package com.rackspace.feeds.utils

import com.sun.org.apache.xpath.internal.XPathAPI
import org.xml.sax.InputSource
import spock.lang.Shared
import spock.lang.Specification

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory

abstract class BaseTest extends Specification {

    @Shared TransformerFactory transformerFactory
    @Shared DocumentBuilder docBuilder

    def setupMoreSpec() { }

    def setupSpec() {

        transformerFactory = TransformerFactory.newInstance()
        docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()

        setupMoreSpec()
    }

    protected def runXPath( String body, String xPath ) {

        def root = docBuilder.parse( new InputSource( new StringReader( body ) ) ).documentElement

        return XPathAPI.eval( root, xPath )
    }

    protected String getStringValue( String body, String xPath ) {

        return runXPath( body, xPath ).str()
    }
}
