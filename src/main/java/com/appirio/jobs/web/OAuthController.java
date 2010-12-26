package com.appirio.jobs.web;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;

/**
 * @author Jeff Douglas (jeff@appirio.com)
 */
@RequestMapping("/oauth/**")
@Controller
public class OAuthController {

	private static final String ACCESS_TOKEN = "ACCESS_TOKEN";
	private static final String INSTANCE_URL = "INSTANCE_URL";

	private String clientId = null;
	private String clientSecret = null;
	private String redirectUri = null;
	private String environment = null;
	private String authUrl = null;
	private String tokenUrl = null;
	private String accessToken = null;
	
	private void init() {
		
    	redirectUri = "https://127.0.0.1:8443/AppirioCareers/oauth/_callback";
    	environment = "https://na5.salesforce.com";
    	// client id and secret from Force.com Remote Access
    	clientId = "YOUR-CLIENT-ID";
        clientSecret = "YOUR-CLIENT-SECRET";
		
        try {
			authUrl = environment + "/services/oauth2/authorize?response_type=code&client_id=" 
				+ clientId + "&redirect_uri=" + URLEncoder.encode(redirectUri, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

        tokenUrl = environment + "/services/oauth2/token";
		
	}
	
	/* Start the OAuth process if no session with the access token is found. 
	 * If a session exists, the redirect the user to the /job/list page. */
	@RequestMapping(value = "/oauth/start", method = RequestMethod.GET)
	public String startOauth(WebRequest req) {
		
		init();
		// check for an access token in the servlet session
		accessToken = (String) req.getAttribute(ACCESS_TOKEN, RequestAttributes.SCOPE_SESSION);

		if (accessToken != null) {
			System.out.println("Session found!! Access token: "+accessToken);
			return "forward:/job/list";
		} else {
			System.out.println("No session... need to auth.");				
			return "redirect:" + authUrl;
		}

	}
	
	/* OAuth callback from Force.com after authrozing the application.  */
	@RequestMapping(value = "/oauth/_callback", method = RequestMethod.GET)
	public String endOauth(WebRequest req) {

		init();
		String accessToken = null;
		String instanceUrl = null;
		String code = req.getParameter("code");
		HttpClient http = new HttpClient();
		PostMethod post = new PostMethod(tokenUrl);
		post.addParameter("client_secret", clientSecret);
		post.addParameter("redirect_uri", redirectUri);
		post.addParameter("grant_type", "authorization_code");
		post.addParameter("code", code);
		post.addParameter("client_id", clientId);
		
		try {
			JSONObject json = null;
			http.executeMethod(post);
			String respBody = post.getResponseBodyAsString();
			System.out.println("post response: " + respBody);
			try {
				json = new JSONObject(respBody);
				accessToken = json.getString("access_token");
				instanceUrl = json.getString("instance_url");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			System.out.println("found access token: "+accessToken);
			System.out.println("found instance url: "+instanceUrl);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			post.releaseConnection();
		}
		
		System.out.println("Setting Access token: "+accessToken);
		System.out.println("Setting Instance Url: "+instanceUrl);
		
		/* Set the token and url to the session so other servlets can access it. */
		req.setAttribute(ACCESS_TOKEN, accessToken, RequestAttributes.SCOPE_SESSION);
		req.setAttribute(INSTANCE_URL, instanceUrl, RequestAttributes.SCOPE_SESSION);
		
		return "forward:/job/list";
	}

	@RequestMapping
	public String index() {
		return "oauth/index";
	}
}
