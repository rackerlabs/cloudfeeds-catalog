package com.rackspace.feeds.feedscatalog.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebController {

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
