var customHttpHeadersConfig = {
	"storage" : {
		"net" : {
			"http" : {
				"headers" : {
					"x-amz-meta-var-var1":"My-Object"
				}
			}
		}
	}
};

Load
	.config(customHttpHeadersConfig)
	.run()
