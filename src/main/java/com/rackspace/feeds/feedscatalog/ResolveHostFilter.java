package com.rackspace.feeds.feedscatalog;

import com.rackspace.feeds.filter.OutputStreamResponseWrapper;
import com.rackspace.feeds.filter.ServletOutputStreamWrapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.*;

public class ResolveHostFilter implements Filter {

    static Logger LOG = LoggerFactory.getLogger(ResolveHostFilter.class);
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder;
    Document doc;

    private String hostFilePath;

    /**
     * This method is called once when the filter is first loaded.
     */
    public void init(FilterConfig filterConfig) throws ServletException {
        LOG.debug("initializing ResolveHostFilter: resolving hostname for feeds catalog.");

        hostFilePath = filterConfig.getInitParameter( "hostFilePath" );

        if ( hostFilePath == null ) {
            throw new ServletException( "hostFilePath parameter is required for this filter" );
        }
    }

    public void doFilter (ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;

        // do chain filter and obtain response content
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ServletOutputStreamWrapper servletOutputStreamWrapper = new ServletOutputStreamWrapper(stream);
        OutputStreamResponseWrapper wrappedResponse =
                new OutputStreamResponseWrapper(httpServletResponse, servletOutputStreamWrapper);

        filterChain.doFilter(servletRequest, wrappedResponse);
        String originalResponseContent = stream.toString();

        // resolve hostname and tenantIds in response if not empty
        if (StringUtils.isNotEmpty(originalResponseContent)) {

            // resolve hostname
            String newResponseContent = resolveHostname(httpServletRequest, originalResponseContent);

            // resolving tenantIds
            IdsFromHeader idsFromHeader = getTenantIds(httpServletRequest);
            newResponseContent = newResponseContent.replace("${nastId}", idsFromHeader.getNastId());
            newResponseContent = newResponseContent.replace("${tenantId}", idsFromHeader.getTenantId());

            // write new content to response output
            httpServletResponse.setHeader("Content-Length", Integer.toString(newResponseContent.getBytes().length));
            httpServletResponse.getOutputStream().write(newResponseContent.getBytes());
        }
    }

    String resolveHostname(HttpServletRequest httpServletRequest, String originalResponseContent) throws ServletException {
        String newContent;

        // resolving hostname
        File file = new File(hostFilePath);
        if (!file.exists()) {
            throw new ServletException("File hostFilePath does not exists at " + hostFilePath);
        }

        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
            doc = documentBuilder.parse(hostFilePath);
            doc.getDocumentElement().normalize();
        }
        catch ( Exception e ) {
            LOG.error( "Error loading feedscatalog.xml from: " + e.getMessage());
            throw new ServletException( e );
        }

        String hostname;
        String externalLocs = httpServletRequest.getHeader("x-external-loc");
        if (externalLocs != null && !externalLocs.isEmpty()) {
            //request from external node, use externalVipURL
            hostname = doc.getDocumentElement().getElementsByTagName("externalVipURL").item(0).getTextContent();
        }
        else {
            //request from internal node, use vipURL
            hostname = doc.getDocumentElement().getElementsByTagName("vipURL").item(0).getTextContent();
        }
        newContent = originalResponseContent.replaceAll("(?i)http://localhost", hostname);

        return newContent;
    }

    public void setHostFilePath(String hostFilePath) {
        this.hostFilePath = hostFilePath;
    }

    /**
     * The counterpart to the init( ) method.
     */
    public void destroy( ) {

    }

    IdsFromHeader getTenantIds(HttpServletRequest request) {
        // return tenantId and nastId from header values
        Enumeration headerValues = request.getHeaders("x-tenant-id");
        ArrayList<String> tenantIds = new ArrayList<String>();
        while (headerValues.hasMoreElements()) {
            tenantIds.add((String) headerValues.nextElement());
        }

        //tenantId is the shortest length value, nastId is the longest length value from header
        IdsFromHeader idsFromHeader = new IdsFromHeader();

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

            idsFromHeader.setTenantId(tenantId);
            idsFromHeader.setNastId(nastId);
        }

        return idsFromHeader;
    }

    class IdsFromHeader {
        private String tenantId = "";
        private String nastId = "";

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getNastId() {
            return nastId;
        }

        public void setNastId(String nastId) {
            this.nastId = nastId;
        }
    }
}
