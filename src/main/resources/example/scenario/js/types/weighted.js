var sharedConcurrency = 100;
var itemDataSize = "10KB";
var itemsFile = "weighted_load_example.csv";
var itemOutputPath = "/weighted_load_example";

// declare the cleanup shell command
new java.lang.ProcessBuilder("/bin/sh", "-c", "rm -f " + itemsFile)
	.inheritIO()
	.start()
	.waitFor();

var sharedConfig = {
	"item": {
		"data": {
			"size": itemDataSize
		},
		"output": {
			"path": itemOutputPath
		}
	},
	"storage": {
		"driver": {
			"limit": {
				"concurrency": sharedConcurrency
			}
		}
	}
}

// prepare (create) the 10000 items on a storage before the test
PreconditionLoad
	.config(sharedConfig)
	.config(
		{
			"item": {
				"output": {
					"file": itemsFile,
				}
			},
			"load": {
				"step": {
					"limit": {
						"count": 10000
					}
				}
			}
		}
	)
	.run();

// declare the weighted load step instance (20% create operations, 80% read operations)
WeightedLoad
	.config(sharedConfig)
	.config(
		{
			"load": {
				"step": {
					"limit": {
						"time": "100s"
					}
				}
			}
		}
	)
	.append(
		{
			"load": {
				"op": {
					"weight": 20
				}
			}
		}
	)
	.append(
		{
			"item": {
				"input": {
					"file": itemsFile
				}
			},
			"load": {
				"op": {
					"recycle": {
						"enabled": true
					},
					"weight": 80
				},
				"type": "read"
			}
		}
	)
	.run();
