package com.rackspace.feeds.feedscatalog;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class WebController {

    @Value("#{feedsCatalogProperties['feeds.region']}")
    private String region;

    @RequestMapping(value = "/catalog/{tenantId}", method = RequestMethod.GET)
    public String observer(@PathVariable String tenantId, ModelMap model) {
        model.addAttribute("tenantId", tenantId);
        model.addAttribute("region", region);
        return "atom_hopper_observer";
    }

    @RequestMapping(value = "/catalog", method = RequestMethod.GET)
    public String cfserviceadmin(ModelMap model) {
        model.addAttribute("region", region);
        return "atom_hopper_identity_admin";
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getRegion() {
        return this.region;
    }

}
