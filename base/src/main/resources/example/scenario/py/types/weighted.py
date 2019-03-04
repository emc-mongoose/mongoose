sharedConcurrency = 100
itemDataSize = "10KB"
itemsFile = "weighted_load_example.csv"
itemOutputPath = "/weighted_load_example"

# declare the cleanup shell command
precondition1 = Command \
	.value("rm -f " + itemsFile)

# prepare (create) the 10000 items on a storage before the test
precondition2 = PreconditionLoad \
	.config(
		{
			"item": {
				"data": {
					"size": itemDataSize
				},
				"output": {
					"file": itemsFile,
					"path": itemOutputPath
				}
			},
			"load": {
				"op": {
					"limit": {
						"count": 10000
					}
				},
				"step": {
					"limit": {
						"concurrency": sharedConcurrency
					}
				}
			}
		}
	)

# declare the weighted load step instance (20% create operations, 80% read operations)
weightedLoad1 = WeightedLoad \
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
	) \
	.append(
		{
			"item": {
				"data": {
					"size": itemDataSize
				},
				"output": {
					"path": itemOutputPath
				}
			},
			"load": {
				"generator": {
					"weight": 20
				}
			}
		}
	) \
	.append(
		{
			"item": {
				"input": {
					"file": itemsFile
				}
			},
			"load": {
				"generator": {
					"recycle": {
						"enabled": True
					},
					"weight": 80
				},
				"type": "read"
			}
		}
	)

# go
precondition1.run()
precondition2.run()
weightedLoad1.run()
