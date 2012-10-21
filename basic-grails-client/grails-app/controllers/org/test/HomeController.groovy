/*
 * Cloud Foundry 2012.02.03 Beta
 * Copyright (c) [2009-2012] VMware, Inc. All Rights Reserved.
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product includes a number of subcomponents with
 * separate copyright notices and license terms. Your use of these
 * subcomponents is subject to the terms and conditions of the
 * subcomponent's license, as noted in the LICENSE file.
 */

package org.test
import grails.converters.JSON
import grails.util.Holders

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.scribe.builder.api.DefaultApi20
import org.scribe.extractors.AccessTokenExtractor
import org.scribe.extractors.JsonTokenExtractor
import org.scribe.model.OAuthConfig
import org.scribe.model.OAuthConstants
import org.scribe.model.OAuthRequest
import org.scribe.model.Response
import org.scribe.model.Token
import org.scribe.model.Verb
import org.scribe.model.Verifier
import org.scribe.oauth.OAuth20ServiceImpl
import org.scribe.oauth.OAuthService

import uk.co.desirableobjects.oauth.scribe.OauthController
import uk.co.desirableobjects.oauth.scribe.OauthService


class HomeController {
	
	GrailsApplication grailsApplication
	
	String getUaaTokenServer() { grailsApplication.config.uaaTokenServer }

	String getUaaLoginServer() { grailsApplication.config.uaaLoginServer?:uaaTokenServer }
	
	String getCloudControllerServer() { grailsApplication.config.cloudControllerServer }

	OauthService oauthService
	
	OauthController oauthController
	
	def beforeInterceptor = [action: this.&auth, except: ['logout', 'callback']]

	private auth() {
		if (!session.user) {
			session.current = request.requestURL
			redirect(uri: "/oauth/uaa/authenticate")
			return false
		}
	}

	def index() {
		render """
<html>
<body>
	<h1>Sample Home Page</h1>
	<p>Welcome ${session.user['user_name']}</p>
	<ul>
		<li><a href="${createLink(uri:'/apps')}">Apps</a></li>
		<li><a href="${createLink(uri:'/logout')}">Logout</a></li>
		<li><a href="${createLink(uri:'/')}">Home</a></li>
	</ul>
	<h3>Technical Information</h3>
	<p>Your principal object is....: ${session.user}</p>
	<p>Your authentication is....: ${session.auth}</p>
</body>
</html>
"""
	}

	def logout() {
		session.invalidate()
		def url = "${request.requestURL}"
		render """
<html>
<body>
	<h1>Logged Out</h1>
	<ul>
		<li><a href="${uaaLoginServer}/logout.do?redirect=${url}">Logout</a> of Cloud Foundry</li>
		<li><a href="${createLink(uri:'/')}">Home</a></li>
	</ul>
</body>
</html>
"""
	}
	
	def apps() {
		def apps = JSON.parse(oauthService.getUaaResource(session.auth, "${cloudControllerServer}/apps").body)
		def tree = new StringBuilder()
		apps.each { app ->
		  def body = new StringBuilder()
		  app.each { k,v ->
			body.append("<li>${k}: ${v}</li>")
		  }
		  tree.append("<li>${app.name}<ul>${body}</ul></li>")
		}
		render """
<html>
  <body>
    <h1>Your Apps</h1>
    <ul>
      <li><a href="${createLink(uri:'/')}">Home</a></li>
    </ul>
    Your Apps:
    <ul id="tree" class="treeview">${tree}</ul>
  </body>
</html>
"""
	  
	}
	
	def callback() {
		session.auth = session[oauthService.findSessionKeyForAccessToken('uaa')]
		session.user = JSON.parse(oauthService.getUaaResource(session.auth, "${uaaTokenServer}/userinfo").body)
		if (session.current) {
			def url = session.current
			session.current = null
			redirect(uri:url)
		} else {
			index()
		}
	}

}

class UaaProvider extends DefaultApi20 {
	
	@Override
	public String getAccessTokenEndpoint() {
		return "${Holders.config.uaaLoginServer}/oauth/token";
	}

	@Override
	public String getAuthorizationUrl(OAuthConfig config) {
		LinkGenerator linkGenerator = Holders.applicationContext.getBean(LinkGenerator)
		def redirectUri = linkGenerator.link(uri: config.callback, absolute: true);
		return "${Holders.config.uaaLoginServer}/oauth/authorize?response_type=code&client_id=${config.apiKey}&redirect_uri=${redirectUri}";
	}
	
	@Override
	public OAuthService createService(OAuthConfig config) {
		return new UaaOauthService(this, config);
	}
	
	@Override
	public AccessTokenExtractor getAccessTokenExtractor() {
		return new JsonTokenExtractor();
	}
}

class UaaOauthService extends OAuth20ServiceImpl {
	
	private DefaultApi20 api
	private OAuthConfig config
	
	UaaOauthService(DefaultApi20 api, OAuthConfig config) {
		super(api, config)
		this.api = api
		this.config = config
	}

	Token getAccessToken(Token requestToken, Verifier verifier)
	{
	  OAuthRequest request = new OAuthRequest(Verb.POST, api.getAccessTokenEndpoint());
	  request.addHeader("Authorization", "Basic " + (config.apiKey + ':' + config.apiSecret).bytes.encodeBase64(false));
	  request.addHeader("Accept", "application/json");
	  request.addQuerystringParameter("grant_type", "authorization_code");
	  request.addQuerystringParameter(OAuthConstants.CODE, verifier.getValue());
	  LinkGenerator linkGenerator = Holders.applicationContext.getBean(LinkGenerator)
	  def redirectUri = linkGenerator.link(uri: config.callback, absolute: true);
	  request.addQuerystringParameter(OAuthConstants.REDIRECT_URI, redirectUri);
	  if(config.hasScope()) request.addQuerystringParameter(OAuthConstants.SCOPE, config.getScope());
	  Response response = request.send();
	  return api.getAccessTokenExtractor().extract(response.getBody());
	}
}
