class UrlMappings {

	static mappings = {
		"/$controller/$action?/$id?"{
			constraints {
				// apply constraints here
			}
		}

		"/"(controller:"home")
		"/callback"(controller:"home", action: "callback")
		"/logout"(controller:"home", action: "logout")
		"/apps"(controller:"home", action: "apps")
	}
}
