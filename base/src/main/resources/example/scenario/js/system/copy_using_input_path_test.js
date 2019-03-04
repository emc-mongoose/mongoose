var sharedConfig = {
	"storage": {
		"namespace": "ns1"
	}
};

PreconditionLoad
	.config(sharedConfig)
	.config(
		{
			"item": {
				"output": {
					"path": ITEM_SRC_PATH
				}
			},
			"load": {
				"op": {
					"limit": {
						"count": 100000
					}
				},
				"step": {
					"limit": {
						"size": "1GB",
						"time": "30s"
					}
				}
			}
		}
	)
	.run();

Load
	.config(sharedConfig)
	.config(
		{
			"item": {
				"input": {
					"path": ITEM_SRC_PATH
				},
				"output": {
					"path": ITEM_DST_PATH
				}
			},
			"load" : {
				"step" : {
					"limit" : {
						"time" : "60s"
					}
				}
			}
		}
	)
	.run();
