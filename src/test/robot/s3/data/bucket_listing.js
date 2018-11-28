PreconditionLoad
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
						"count": 10000
					}
				},
			}
		}
	)
	.run();

Load
	.config(
		{
			"item": {
				"input": {
					"path": ITEM_SRC_PATH
				},
				"output": {
					"path": ITEM_DST_PATH
				}
			}
		}
	)
	.run();
