Load
	.config(
		{
			"load" : {
				"op" : {
					"limit" : {
						"count" : 1000000,
						"rate" : 1000
					}
				},
				"step" : {
					"limit" : {
						"time" : "1m"
					}
				}
			},
			"storage" : {
				"driver" : {
					"limit" : {
						"concurrency" : 0
					}
				}
			}
		}
	)
	.run();
