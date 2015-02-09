package com.rackspace.feeds.feedscatalog;

import com.rackspace.feeds.filter.OutputStreamResponseWrapper;
import com.rackspace.feeds.filter.ServletOutputStreamWrapper;
import com.rackspace.feeds.filter.TransformerUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.URL;
import java.util.*;

public class ResolveHostFilter implements Filter {

    static Logger LOG = LoggerFactory.getLogger(ResolveHostFilter.class);
    static final String resolveHostXslt = "feedscatalog-resolve-host.xsl";

    private TransformerUtils transformer;

    /**
     * This method is called once when the filter is first loaded.
     */
    public void init(FilterConfig filterConfig) throws ServletException {
        LOG.debug( "initializing ResolveHostFilter" );

        try {
            //load the resolve host xslt from resource
            URL xsltResource = Thread.currentThread().getContextClassLoader().getResource(resolveHostXslt);
            if (xsltResource != null) {
                transformer = TransformerUtils.getInstanceForXsltAsFile(xsltResource.getPath());
            }
            else {
                throw new UnavailableException("resource is null - " + resolveHostXslt);
            }
        } catch ( Exception e ) {
            LOG.error( "Error loading xslt: " + e.getMessage());
            throw new ServletException( e );
        }
    }

    public void doFilter (ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        // setup wrapper response with output stream to collect transformed content
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ServletOutputStreamWrapper outputStreamWrapper = new ServletOutputStreamWrapper(stream);
        OutputStreamResponseWrapper wrappedResponse =
                new OutputStreamResponseWrapper(httpServletResponse, outputStreamWrapper);

        // apply filter further down the chain on wrapped response
        chain.doFilter(httpServletRequest, wrappedResponse);

        // obtain response content
        String originalResponseContent = stream.toString();
        LOG.debug("Original response content length = " + originalResponseContent.length());

        if (StringUtils.isNotEmpty(originalResponseContent)) {

            // create input params to xslt with headers from request
            HashMap<String, Object> params = new HashMap<String, Object>();
            putTenantIdParams(httpServletRequest, params);
            putExternalLocParams(httpServletRequest, params);

            try {
                OutputStream outputStream = new ByteArrayOutputStream();

                // transform response content with resolve host xslt
                transformer.doTransform(params,
                    new StreamSource(new StringReader(originalResponseContent)),
                    new StreamResult(outputStream));

                // set transformed content to response
                String newResponseContent = outputStream.toString();
                LOG.debug("New response content length = " + newResponseContent.length());
                httpServletResponse.setContentLength(newResponseContent.length());
                httpServletResponse.getWriter().write(newResponseContent);
            }
            catch (Exception e) {
                LOG.error("Error transforming xml: " + e.getMessage());
            }
            finally {
                httpServletResponse.getWriter().close();
            }
        }
    }

    /**
     * The counterpart to the init( ) method.
     */
    public void destroy( ) {

    }

    void putTenantIdParams(HttpServletRequest request, HashMap<String, Object> params) {
        //put tenantId and nastId into the params HashMap
        //tenantId is the shortest length value, nastId is the longest length value
        Enumeration headerValues = request.getHeaders("x-tenant-id");
        ArrayList<String> tenantIds = new ArrayList<String>();
        while (headerValues.hasMoreElements()) {
            tenantIds.add((String) headerValues.nextElement());
        }

        if (tenantIds.size() > 0) {
            String tenantId = tenantIds.get(0);
            String nastId = tenantIds.get(0);

            for (String s: tenantIds) {
                if (s.length() < tenantId.length()) {
                    tenantId = s;
                }

                if (s.length() > nastId.length()) {
                    nastId = s;
                }
            }

            params.put("tenantId", tenantId);
            params.put("nastId", nastId);
        }
    }

    void putExternalLocParams(HttpServletRequest request, HashMap<String, Object> params) {
        //put x-external-loc header into the params HashMap if exists
        String externalLocs = request.getHeader("x-external-loc");
        if (externalLocs != null && !externalLocs.isEmpty()) {
            params.put("externalLoc", externalLocs);
        }
    }
}
