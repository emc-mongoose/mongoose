package example.scenario.groovy
// increase the concurrency level by the factor of 10
final limitConcurrencyFactor = 10

final itemDataSizes = [
	"10KB", "1MB", "100MB", "10GB"
]

final limitCount = 1000000;
final limitTime = 100;
// initial concurrency limit value
int limitConcurrency = 1;

static def configLimitConcurrency(c) {
	return [
		load: [
			step: [
				limit: [
					concurrency: c
				]
			]
		]
	]
}

static def configItemDataSize(s) {
	return [
		item: [
			data: [
				size: s
			]
		]
	]
}

static def configCreate(iterId, size, itemOutputFile, limitCount, limitTime) {
	return [
		item: [
			data : [
				size : size
			],
			output: [
				file : itemOutputFile,
				path : "/default"
			]
		],
		load: [
			op: [
				limit: [
					count: limitCount
				]
			],
			step: [
				id: "create0_$iterId".toString(),
				limit: [
					time: limitTime
				]
			]
		]
	]
}

static def configRead(iterId, itemInputFile) {
	return [
		item: [
			input: [
				file: itemInputFile
			]
		],
		load: [
			step: [
				id: "read1_$iterId".toString()
			]
		]
	]
}

static def configUpdate(iterId, itemInputFile, itemOutputFile) {
	return [
		item: [
			input: [
				file: itemInputFile
			],
			output: [
				file: itemOutputFile
			]
		],
		load: [
			step: [
				id: "update2_$iterId".toString()
			]
		]
	]
}

static def configReadPartial(iterId, itemInputFile) {
	return [
		item: [
			input: [
				file: itemInputFile
			]
		],
		load: [
			step: [
				id: "read3_$iterId".toString()
			]
		]
	]
}

static def configDelete(iterId, itemInputFile) {
	return [
		item: [
			input: [
				file: itemInputFile
			]
		],
		load: [
			step: [
				id: "delete4_$iterId".toString()
			]
		]
	]
}

// typically OS open files limit is 1024 so won't try to use the concurrency level higher than that
while(limitConcurrency < 1024) {

	for(final String itemDataSize : itemDataSizes) {

		println(
			("Run the load steps using the concurrency limit of $limitConcurrency"
				+ " and items data size $itemDataSize").toString()
		)

		final nextConfigLimitConcurrency = configLimitConcurrency(limitConcurrency)
		final iterId = "concurrency${limitConcurrency}_size$itemDataSize".toString()
		final iterItemsFile0 = "items_${iterId}_0.csv".toString()
		final iterItemsFile11 = "items_${iterId}_1.csv".toString()

		CreateLoad
			.config(nextConfigLimitConcurrency)
			.config(configItemDataSize(itemDataSize))
			.config(
				configCreate(
					iterId, itemDataSize, iterItemsFile0, limitCount, limitTime
				)
			)
			.run()

		ReadLoad
			.config(nextConfigLimitConcurrency)
			.config(configRead(iterId, iterItemsFile0))
			.run()

		UpdateRandomRangeLoad
			.config(nextConfigLimitConcurrency)
			.config(configUpdate(iterId, iterItemsFile0, iterItemsFile11))
			.run()

		ReadVerifyRandomRangeLoad
			.config(nextConfigLimitConcurrency)
			.config(configReadPartial(iterId, iterItemsFile11))
			.run()

		DeleteLoad
			.config(nextConfigLimitConcurrency)
			.config(configDelete(iterId, iterItemsFile0))
			.run()
	}

	limitConcurrency *= limitConcurrencyFactor
}
