Load
	.config(
		{
			"load": {
				"limit": {
					"count": 100
				},
				"threads": 10
			},
			"run": {
				"id": "backward-compatibility-test"
			},
			"storage": {
				"auth": {
					"id": "wuser1@sanity.local"
				},
				"node": {
					"addrs": new java.util.ArrayList(["127.0.0.1"])
				}
			}
		}
	)
	.run();
