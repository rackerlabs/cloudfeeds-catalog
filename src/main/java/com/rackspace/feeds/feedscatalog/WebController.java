package com.rackspace.feeds.feedscatalog;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class WebController {

    @RequestMapping(value = "/{tenantId}", method = RequestMethod.GET)
    public String observer(@PathVariable String tenantId, ModelMap model) {
        model.addAttribute("tenantId", tenantId);
        return "atom_hopper_observer";
    }

    @RequestMapping(value = "/.*", method = RequestMethod.GET)
    public String cfserviceadmin(ModelMap model) {
        return "atom_hopper_identity_admin";
    }

}
