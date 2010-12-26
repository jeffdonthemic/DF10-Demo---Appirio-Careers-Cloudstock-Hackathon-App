package com.appirio.jobs.web;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod; 
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;

import com.appirio.jobs.domain.Job;
import com.appirio.jobs.domain.SmsMessage;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.TwilioRestResponse;

/**
 * @author Jeff Douglas (jeff@appirio.com)
 */

@RequestMapping("/job/**")
@Controller
public class JobController {
	
	private static final String ACCESS_TOKEN = "ACCESS_TOKEN";
	private static final String INSTANCE_URL = "INSTANCE_URL";
	private static Mongo m;
	private static DB db;
	private static DBCollection coll;
	private static DBCursor cur;
    ArrayList<Job> jobs = new ArrayList<Job>();
    
    
    @RequestMapping(value="/job/list", method=RequestMethod.GET)
    public ModelAndView list(WebRequest req) {
    	
    	// if the job collection is empty then fetch jobs from Force.com
    	if (jobs.isEmpty()) {
    		
    		// fetch the access token and url from the servlet session
    		String accessToken = (String) req.getAttribute(ACCESS_TOKEN, RequestAttributes.SCOPE_SESSION);
    		String instanceUrl = (String) req.getAttribute(INSTANCE_URL, RequestAttributes.SCOPE_SESSION);
    		
    		System.out.println("Access token: "+accessToken);
    		System.out.println("Instance Url: "+instanceUrl);
    		    			
    		jobs = new ArrayList<Job>();
			HttpClient httpclient = new HttpClient();
	        GetMethod gm = new GetMethod(instanceUrl + "/services/data/v20.0/query");
	        //set the token in the header
	        gm.setRequestHeader("Authorization", "OAuth "+accessToken);
	        //set the SOQL as a query param
	        NameValuePair[] params = new NameValuePair[1];
	        //no need to url encode here...it will cause your query to fail
	        params[0] = new NameValuePair("q","Select Id, Name, Job_Title__c, Location__c, " +
	        		"Duties__c, Skills__c, Salary__c, Box_Url__c from Job__c Order by Job_Title__c");
	        gm.setQueryString(params);
	        
	        String respBody = "";
	
	        try {
				httpclient.executeMethod(gm);
				respBody = gm.getResponseBodyAsString();
		        System.out.println("response body: " + respBody);
			} catch (HttpException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			} finally {
	            gm.releaseConnection();
	        }
	
	        try {
	            JSONObject json = new JSONObject(respBody);                
	            JSONArray results = json.getJSONArray("records");
	            
	           for(int i = 0; i < results.length(); i++) {
	        	   // add the json payload to a Job object
	        	   Job job = new Job(results.getJSONObject(i).getString("Id"), 
						results.getJSONObject(i).getString("Name"),
						results.getJSONObject(i).getString("Job_Title__c"),
						results.getJSONObject(i).getString("Location__c"),
						results.getJSONObject(i).getString("Duties__c"),
						results.getJSONObject(i).getString("Skills__c"),
						results.getJSONObject(i).getString("Salary__c"),
						results.getJSONObject(i).getString("Box_Url__c"));
	        	   
	        	   // add the job to the collection
	        	   jobs.add(job);
	           }
	           
	           
	        } catch (JSONException e) {
	            e.printStackTrace();
	        }
	        
	        System.out.println("jobs found: "+jobs.size());

    	}
    	
    	ModelAndView mav = new ModelAndView("job/list");
    	mav.addObject("jobs", jobs);
    	return mav;
    }
	
    @RequestMapping(value="/job/{id}/display", method=RequestMethod.GET)
    public ModelAndView display(@PathVariable String id, Model model) {
    	Job job = getJobById(id);
    	incrementCount(job.getName(),"views");
    	ModelAndView mav = new ModelAndView("job/display");
    	mav.addObject(getJobById(id));
    	return mav;
    }
    
    @RequestMapping(value="/job/{id}/sms", method=RequestMethod.GET)
    public ModelAndView message(@PathVariable String id, Model model) {
    	ModelAndView mav = new ModelAndView("job/sms"); 
    	SmsMessage sms = new SmsMessage();
    	sms.setPhone("9412274843");
    	sms.setMessage("Check out this AWESOME job with Appirio!");
    	mav.addObject("smsMessage", sms);
    	mav.addObject(getJobById(id));
    	return mav;
    }
    
    @RequestMapping(value="/job/{id}/print", method=RequestMethod.GET)
    public String print(@PathVariable String id, Model model) {
    	Job job = getJobById(id);
    	incrementCount(job.getName(),"downloads");
    	System.out.println(job.getBoxUrl());
    	return "redirect:"+job.getBoxUrl();
    }
    
    @RequestMapping(value = "/job/{id}/smsSend", method = RequestMethod.POST)
    public ModelAndView smsSubmit(@PathVariable String id, @ModelAttribute SmsMessage sms, Model model) {
    	
    	Job job = getJobById(id);
    	sendSms(job, sms.getPhone(), sms.getMessage());
    	incrementCount(job.getName(),"messages");
    	
    	ModelAndView mav = new ModelAndView("job/smsConfirm"); 
    	mav.addObject("phone", sms.getPhone());
    	mav.addObject("message", sms.getMessage());
    	mav.addObject(job);
    	return mav;

    }
    
	private void incrementCount(String name, String type) {
		
		try {
			m = new Mongo("flame.mongohq.com", 27065);
			db = m.getDB("AppirioCareers");
			char[] password = { '4','+','r','E','o','x','x','x','x','x'};
			boolean auth = db.authenticate("jeffdonthemic", password);
			System.out.println("Mongo auth?: "+auth);
			coll = db.getCollection("jobs");	
		}
		catch (UnknownHostException ex) {
			ex.printStackTrace();
		}
		catch (MongoException ex) {
			ex.printStackTrace();
		}
		
		cur = coll.find(new BasicDBObject("name", name));	
		while (cur.hasNext()) {
			BasicDBObject doc = (BasicDBObject)cur.next();
			if (type.equals("views"))
				doc.put("views", (Integer)doc.get("views")+1);
			else if (type.equals("messages"))
				doc.put("messages", (Integer)doc.get("messages")+1);
			else
				doc.put("downloads", (Integer)doc.get("downloads")+1);
			coll.update( new BasicDBObject("name", name), doc );
		}
		
	}
    
    private void sendSms(Job job, String phone, String message) {
    	
    	String AccountSid = "YOUR-ACCOUNT-ID";
        String AuthToken = "YOUR-AUTH-TOKEN";
        String ApiVersion = "2010-04-01";
        
        TwilioRestClient client = new TwilioRestClient(AccountSid, AuthToken, null);
        
        String msg = "\n"+message+"\n"+job.getJobTitle()+"\nhttp://appirio.com/careers";
        
        System.out.println("size: "+msg.length());
        
        //build map of post parameters 
        Map<String,String> params = new HashMap<String,String>();
        params.put("From", "14155992671");
        params.put("To", phone);
        params.put("Body", msg);
        TwilioRestResponse response;
        try {
            response = client.request("/"+ApiVersion+"/Accounts/"+AccountSid+"/SMS/Messages", "POST", params);
        
            if(response.isError())
                System.out.println("Error making outgoing call: "+response.getHttpStatus()+"\n"+response.getResponseText());
            else {
                System.out.println(response.getResponseText());

            }
        } catch (TwilioRestException e) {
            e.printStackTrace();
        }
    	
    }
    
    private Job getJobById(String id) {
    	Job job = null;
    	for (Job j : jobs) {
    		if (j.getId().equals(id)) {
    			job = j;
    			break;
    		}
    	}
    	return job;
    }
    
    @RequestMapping
    public String index() {
        return "job/index";
    }
    
}
