package com.appirio.jobs.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author Jeff Douglas (jeff@appirio.com)
 */

@RequestMapping("/home/**")
@Controller
public class HomeController {

    @RequestMapping
    public String index() {
        return "home/index";
    }
        	
    
    
}
