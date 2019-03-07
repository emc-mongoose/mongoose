// increase the concurrency level by the factor of 10
var limitConcurrencyFactor = 10;

var itemDataSizes = [
	"10KB", "1MB", "100MB", "10GB"
];

var limitCount = 1000000;
var limitTime = 100;
// initial concurrency limit value
var limitConcurrency = 1;

function configLimitConcurrency(c) {
	return {
		"storage": {
			"driver": {
				"limit": {
					"concurrency": c
				}
			}
		}
	}
};

function configItemDataSize(s) {
	return {
		"item": {
			"data": {
				"size": s
			}
		}
	}
};

function configCreate(iterId, size, itemOutputFile, limitCount, limitTime) {
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
					"count": limitCount,
					"time": limitTime
				}
			}
		}
	}
};

function configRead(iterId, itemInputFile) {
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
};

function configUpdate(iterId, itemInputFile, itemOutputFile) {
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
};

function configReadPartial(iterId, itemInputFile) {
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
};

function configDelete(iterId, itemInputFile) {
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
};

// typically OS open files limit is 1024 so won't try to use the concurrency level higher than that
while(limitConcurrency < 1024) {

	for(var i = 0; i < itemDataSizes.length; i ++) {

		itemDataSize = itemDataSizes[i];
		print(
			"Run the load steps using the concurrency limit of " + limitConcurrency
				+ " and items data size " + itemDataSize
		);
		var nextConfigLimitConcurrency = configLimitConcurrency(limitConcurrency);
		var iterId = "concurrency" + limitConcurrency + "_size" + itemDataSize;
		var iterItemsFile0 = "items_" + iterId + "_0.csv";
		var iterItemsFile11 = "items_" + iterId + "_1.csv";

		CreateLoad
			.config(nextConfigLimitConcurrency)
			.config(configItemDataSize(itemDataSize))
			.config(
				configCreate(
					iterId, itemDataSize, iterItemsFile0, limitCount, limitTime
				)
			)
			.run();

		ReadLoad
			.config(nextConfigLimitConcurrency)
			.config(configRead(iterId, iterItemsFile0))
			.run();

		UpdateRandomRangeLoad
			.config(nextConfigLimitConcurrency)
			.config(configUpdate(iterId, iterItemsFile0, iterItemsFile11))
			.run();

		ReadVerifyRandomRangeLoad
			.config(nextConfigLimitConcurrency)
			.config(configReadPartial(iterId, iterItemsFile11))
			.run();

		DeleteLoad
			.config(nextConfigLimitConcurrency)
			.config(configDelete(iterId, iterItemsFile0))
			.run();
	}

	limitConcurrency *= limitConcurrencyFactor;
	// cast limitConcurrency value to int
	limitConcurrency = ~~limitConcurrency;
}
