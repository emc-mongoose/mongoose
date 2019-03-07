# increase the concurrency level by the factor of 10
limitConcurrencyFactor = 10

itemDataSizes = [
	"10KB", "1MB", "100MB", "10GB"
]

limitCount = 1000000
limitTime = 100
# initial concurrency limit value
limitConcurrency = 1

def configLimitConcurrency(c):
	return {
		"load": {
			"step": {
				"limit": {
					"concurrency": c
				}
			}
		}
	}

def configItemDataSize(s):
	return {
		"item": {
			"data": {
				"size": s
			}
		}
	}

def configCreate(iterId, size, itemOutputFile, limitCount, limitTime):
	return {
		"item": {
			"data": {
				"size": size
			},
			"output": {
				"file": itemOutputFile,
				"path": "/default"
			}
		},
		"load": {
			"op": {
				"limit": {
					"count": limitCount
				}
			},
			"step": {
				"id": "create0_" + iterId,
				"limit": {
					"time": limitTime
				}
			}
		}
	}

def configRead(iterId, itemInputFile):
	return {
		"item": {
			"input": {
				"file": itemInputFile
			}
		},
		"load": {
			"step": {
				"id": "read1_" + iterId
			}
		}
	}

def configUpdate(iterId, itemInputFile, itemOutputFile):
	return {
		"item": {
			"input": {
				"file": itemInputFile
			},
			"output": {
				"file": itemOutputFile
			}
		},
		"load": {
			"step": {
				"id": "update2_" + iterId
			}
		}
	}

def configReadPartial(iterId, itemInputFile):
	return {
		"item": {
			"input": {
				"file": itemInputFile
			}
		},
		"load": {
			"step": {
				"id": "read3_" + iterId
			}
		}
	}

def configDelete(iterId, itemInputFile):
	return {
		"item": {
			"input": {
				"file": itemInputFile
			}
		},
		"load": {
			"step": {
				"id": "delete4_" + iterId
			}
		}
	}

# typically OS open files limit is 1024 so won't try to use the concurrency level higher than that
while limitConcurrency < 1024:

	for i in range(0, len(itemDataSizes)):

		itemDataSize = itemDataSizes[i]
		print "Run the load steps using the concurrency limit of " + str(limitConcurrency) \
			+ " and items data size " + itemDataSize
		nextConfigLimitConcurrency = configLimitConcurrency(limitConcurrency)
		iterId = "concurrency" + str(limitConcurrency) + "_size" + itemDataSize
		iterItemsFile0 = "items_" + iterId + "_0.csv"
		iterItemsFile11 = "items_" + iterId + "_1.csv"

		CreateLoad \
			.config(nextConfigLimitConcurrency) \
			.config(configItemDataSize(itemDataSize)) \
			.config(
				configCreate(
					iterId, itemDataSize, iterItemsFile0, limitCount, limitTime
				)
			) \
			.run()

		ReadLoad \
			.config(nextConfigLimitConcurrency) \
			.config(configRead(iterId, iterItemsFile0)) \
			.run()

		UpdateRandomRangeLoad \
			.config(nextConfigLimitConcurrency) \
			.config(configUpdate(iterId, iterItemsFile0, iterItemsFile11)) \
			.run()

		ReadVerifyRandomRangeLoad \
			.config(nextConfigLimitConcurrency) \
			.config(configReadPartial(iterId, iterItemsFile11)) \
			.run()

		DeleteLoad \
			.config(nextConfigLimitConcurrency) \
			.config(configDelete(iterId, iterItemsFile0)) \
			.run()

	limitConcurrency *= limitConcurrencyFactor
