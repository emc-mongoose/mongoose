Load
	.config(
		{
			"load" : {
				"op" : {
					"limit" : {
						"rate" : 1000
					}
				},
				"step" : {
					"limit" : {
						"count" : 1000000,
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
