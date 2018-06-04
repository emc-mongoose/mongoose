Command
	.value("rm -f " + ITEM_LIST_FILE)
	.run();

PreconditionLoad
	.config(
		{
			"item": {
				"data" : {
					"size": ITEM_DATA_SIZE
				},
				"output": {
					"file": ITEM_LIST_FILE,
					"path": ITEM_OUTPUT_PATH + "/%p{16;2}"
				}
			},
			"test": {
				"step": {
					"limit": {
						"count": STEP_LIMIT_COUNT
					}
				}
			}
		}
	)
	.run();

ReadVerifyLoad
	.config(
		{
			"item": {
				"input": {
					"file": ITEM_LIST_FILE
				}
			}
		}
	)
	.run();
