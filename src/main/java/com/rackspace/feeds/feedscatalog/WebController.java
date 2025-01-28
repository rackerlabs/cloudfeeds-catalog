package com.rackspace.feeds.feedscatalog;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class WebController {

    @RequestMapping(value = "/catalog/catalog-buildinfo", method = RequestMethod.GET)
    public String buildinfo(ModelMap model) {
        return "catalog-buildinfo";
    }

    @RequestMapping(value = "/catalog/{tenantId}", method = RequestMethod.GET)
    public String observer(@PathVariable String tenantId, ModelMap model) {
        model.addAttribute("tenantId", tenantId);
        return "allfeeds_observer";
    }

    @RequestMapping(value = "/catalog", method = RequestMethod.GET)
    public String cfserviceadmin(ModelMap model) {
        return "allfeeds";
    }

}
