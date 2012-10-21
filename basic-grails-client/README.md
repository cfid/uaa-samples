<link href="https://raw.github.com/clownfart/Markdown-CSS/master/markdown.css" rel="stylesheet"></link>
# Minimal CloudFoundry UAA Grails Sample

This project provides a minimal OAuth2 Client Application implemented
in Grails.  It provides 2 protected resources:

* `/`: a home page that lists the details of the currently
  authenticated user
* `/apps`: a list of your Cloud Foundry deployed applications (or a
  fake version of that if you are running everything locally)
  
Those 2 resources are protected - you won't be able to visit them in a
browser until you have authenticated - and authentication is delegated
to the Cloud Foundry Login Server (or the UAA if there isn't one).
  
## Running the Application

Pre-requisites:

* a UAA instance running somewhere
* an OAuth2 client registration (id and secret)
* a Resource Server (either a Cloud Controller or something with an
`/apps` endpoint that behaves like one)
* a user account on the
UAA to test the application.  

The easiest way to do this is to run the UAA and the sample api app
from the command line as per the instructions in its
[README](https://github.com/cloudfoundry/uaa):

    $ git clone git@github.com:cloudfoundry/uaa.git
    $ cd uaa
    $ mvn install
    $ cd samples/api
    $ mvn tomcat7:run -P integration

The UAA should then be running on port 8080:
[http://localhost:8080/uaa](), with the API app next to it.

You can run the Grails application from the command line with Grails
2.1.1:

    $ cd grapps
    $ grails -Dgrails.server.port.http=9080 RunApp

If you visit the app on [http://localhost:9080/uaa]() it should then
redirect you to the UAA and you can log in, approve the access token
grant and then see the sample app home page.  The username and
password are `marissa` and `koala`.

## Externalized Configuration

The application responds to some environment variables:

* `UAA_PROFILE` is used to select a properties file at the root of the
classpath.  This can be used to define the endpoint urls in the UAA
and API resource.
* `CLIENT_ID` is the client id in the UAA registration.  Defaults to
`app`.
* `CLIENT_SECRET` is the client id in the UAA registration.  Defaults to
`appclientsecret`.

The sample comes with a `cloud` profile that points to
`cloudfoundry.com`, and a `vcap` profile that points to `vcap.me`. If
you set `UAA_PROFILE` before you run Grails then you will be able to
use your Cloud Foundry account credentials, but unless you have a
client registration for the client id and secret you won't be able to
get an access token and actually authenticate.
