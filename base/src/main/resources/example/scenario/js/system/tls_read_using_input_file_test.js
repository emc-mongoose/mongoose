var sharedConfig = {
	"storage": {
		"namespace": "ns1",
		"net": {
			"ssl": true
		}
	}
}

new java.lang.ProcessBuilder()
	.command("sh", "-c", "rm -f " + ITEM_LIST_FILE)
	.start()
	.waitFor()

PreconditionLoad
	.config(sharedConfig)
	.config(
		{
			"item": {
				"output": {
					"file": ITEM_LIST_FILE
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
						"size": "100GB",
						"time": 100
					}
				}
			}
		}
	)
	.run()

ReadLoad
	.config(sharedConfig)
	.config(
		{
			"item": {
				"input": {
					"file": ITEM_LIST_FILE
				}
			}
		}
	)
	.run()
