var cmd = new java.lang.ProcessBuilder()
	.command("sh", "-c", "rm -f " + FILE_NAME)
	.inheritIO()
	.start();
cmd.waitFor();

PreconditionLoad
	.config(
		{
			"load" : {
				"op" : {
					"limit" : {
						"count" : 1
					}
				},
				"step" : {
					"node" : {
						"addrs" : null // disable the distributed mode here to write the single item
					}
				}
			},
			"item" : {
				"output" : {
					"file" : FILE_NAME
				}
			}
		}
	)
	.run();

ReadLoad
	.config(
		{
			"load" : {
				"op" : {
					"type" : "read",
					"recycle" : true
				},
				"step" : {
					"limit" : {
						"time" : "1m"
					}
				}
			},
			"item" : {
				"input" : {
					"file" : FILE_NAME
				}
			}
		}
	)
	.run();
