require 'sinatra'
require 'base64'
require 'yajl'
require 'omniauth-oauth2'
require 'restclient'
  
enable :sessions

# URL of the uaa token server
UAA_TOKEN_SERVER = ENV['UAA_TOKEN_SERVER'] || "http://localhost:8080/uaa"
 
# URL of the uaa login (SSO) server
UAA_LOGIN_SERVER = ENV['UAA_LOGIN_SERVER'] || UAA_TOKEN_SERVER
 
# URL of the cloud_controller
CLOUD_CONTROLLER_SERVER = ENV['CLOUD_CONTROLLER_SERVER'] || "http://localhost:8080/api"
 
# CLIENT_ID
CLIENT_ID = ENV['CLIENT_ID'] || "app"

# CLIENT_SECRET
CLIENT_SECRET = ENV['CLIENT_SECRET'] || "appclientsecret"

use OmniAuth::Builder do
  provider :oauth2, CLIENT_ID, '', :client_options => client_options, :token_params => token_params
end

before do
  unprotected = ['/auth/oauth2/callback', '/logout']
  if !unprotected.include?(request.path_info) then
     redirect '/auth/oauth2' unless session[:auth]
  end
end

get '/' do
  <<-HTML
<html>
<body>
	<h1>Sample Home Page</h1>
	<p>Welcome #{session[:user]["user_name"]}</p>
	<ul>
		<li><a href="/apps">Apps</a></li>
		<li><a href="/logout">Logout</a></li>
		<li><a href="/">Home</a></li>
	</ul>
	<h3>Technical Information</h3>
	<p>Your principal object is....: #{session[:user]}</p>
	<p>Your authentication is....: #{session[:auth].to_hash}</p>
</body>
</html>
  HTML
end
  
get '/apps' do
  token = session[:auth][:credentials][:token]
  apps = Yajl::Parser.new.parse(RestClient.get("#{CLOUD_CONTROLLER_SERVER}/apps", :authorization=>"Bearer #{token}"))
  tree = ""
  apps.each do |app|
    body = ""
    app.each do |k,v| 
      body << "<li>#{k}: #{v}</li>"
    end
    tree << "<li>#{app[:name]}<ul>#{body}</ul></li>"
  end
  <<-HTML
<html>
<body>
<h1>Your Apps</h1>
	<ul>
		<li><a href="/">Home</a></li>
	</ul>
Your Apps:
    <ul id="tree" class="treeview">#{tree}</ul>
</body>
</html>
  HTML
end

get '/logout' do
  session.delete(:auth)
  <<-HTML
<html>
<body>
	<h1>Logged Out</h1>
	<ul>
		<li><a href="#{UAA_LOGIN_SERVER}/logout.do?redirect=#{url('logout')}">Logout</a> of Cloud Foundry</li>
		<li><a href="/">Home</a></li>
	</ul>
</body>
</html>
  HTML
end

get '/auth/oauth2/callback' do
  auth = request.env['omniauth.auth']
  session[:auth] = auth
  token = auth[:credentials][:token]
  session[:user] = Yajl::Parser.new.parse(RestClient.get("#{UAA_TOKEN_SERVER}/userinfo", :authorization=>"Bearer #{session[:token]}"))
  status, headers, body = call env.merge("PATH_INFO" => '/')
  [status, headers, body]
end

def client_options
  site,context = extract_site(UAA_LOGIN_SERVER)
  { :site => site, 
    :authorize_url => "#{context}/oauth/authorize", 
    :token_url => "#{context}/oauth/token" }
end

def token_params
  # The oauth2 gem puts client credentials in the form body but the
  # UAA requires them in a header (as recommended in the OAuth2 spec).
  basic = Base64.encode64("#{CLIENT_ID}:#{CLIENT_SECRET}")
  { :headers => {:authorization => "Basic #{basic}"} }
end

def extract_site(url)
  # The oauth2 gem can't deal with a target URL for the token server,
  # so we have to split it up into site and context path.
  site = url
  site = "http://#{site}" unless site.start_with? "http"
  uri = URI(site)
  context = uri.path=="/" ? "" : uri.path
  site = "#{uri.scheme}://#{uri.host}"
  site = "#{site}:#{uri.port}" if uri.port!=80 && uri.port!=443
  [site,context]
end

