package com.rackspace.feeds.feedscatalog.filter;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rackspace.feeds.filter.StringResponseWrapper;
import com.rackspace.feeds.filter.TransformerUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * Resolve the hostname vip and tenantIds of href links in the feeds catalog response
 */
public class ResolveHostFilter implements Filter {

    static Logger LOG = LoggerFactory.getLogger(ResolveHostFilter.class);
    static final String resolveHostXslt = "feedscatalog-resolve-host.xsl";

    private TransformerUtils transformer;

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

        // setup wrapper response with output stream to collect response content
        StringResponseWrapper wrappedResponse = new StringResponseWrapper(httpServletResponse);

        // apply filter further down the chain on wrapped response
        chain.doFilter(httpServletRequest, wrappedResponse);

        // obtain response content
        String originalResponseContent = wrappedResponse.getResponseString();

        // transform the response with resolve-host if not empty
        if (StringUtils.isNotEmpty(originalResponseContent)) {

            // create input params to xslt with headers from request
            HashMap<String, Object> params = new HashMap<String, Object>();
            putTenantIdParams(httpServletRequest, params);
            putExternalLocParams(httpServletRequest, params);

            try {
                // create outputStream to store result of transformation
                OutputStream outputStream = new ByteArrayOutputStream();

                // transform response content with resolve-host xslt
                transformer.doTransform(params,
                    new StreamSource(new StringReader(originalResponseContent)),
                    new StreamResult(outputStream));

                // set transformed content to response
                String newResponseContent = outputStream.toString();
                httpServletResponse.setContentLength(newResponseContent.length());
                httpServletResponse.getWriter().write(newResponseContent);
            }
            catch (Exception e) {
                LOG.error("Error transforming xml:" + e.getMessage());
                throw new ServletException(e);
            }
            finally {
                httpServletResponse.getWriter().close();
            }
        }
    }

    public void destroy( ) {

    }

    /**
     * put tenantId and nastId into the HashMap params
     *
     * @param request
     * @param params
     */
    void putTenantIdParams(HttpServletRequest request, HashMap<String, Object> params) {
        // tenantId is the shortest length value, nastId is the longest length value
        String tenantId = null;
        String nastId = null;
        Enumeration headerValues = request.getHeaders("x-tenant-id");
        ArrayList<String> tenantIds = new ArrayList<String>();
        while (headerValues.hasMoreElements()) {
            tenantIds.add((String) headerValues.nextElement());
        }

        if (tenantIds.size() > 0) {
            tenantId = tenantIds.get(0);
            nastId = tenantIds.get(0);

            for (String s: tenantIds) {
                if (s.length() < tenantId.length()) {
                    tenantId = s;
                }

                if (s.length() > nastId.length()) {
                    nastId = s;
                }
            }
        }
        params.put("tenantId", tenantId);
        params.put("nastId", nastId);
    }

    /**
     * put x-external-loc header into the HashMap params if the header exists
     * @param request
     * @param params
     */
    void putExternalLocParams(HttpServletRequest request, HashMap<String, Object> params) {
        String externalLocs = request.getHeader("x-external-loc");
        if (externalLocs != null && !externalLocs.isEmpty()) {
            params.put("externalLoc", externalLocs);
        }
        else {
            params.put("externalLoc", null);
        }
    }
}
