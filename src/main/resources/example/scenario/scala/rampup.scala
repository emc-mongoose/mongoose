object rampup {

	// increase the concurrency level by the factor of 10
	val limitConcurrencyFactor = 10

	val itemDataSizes = Seq("10KB", "1MB", "100MB", "10GB")

	val limitCount = 1000000
	val limitTime = 100

	def configLimitConcurrency(c: Int):
	Map[String, Any] = {
		Map(
			"load" -> Map(
				"step" -> Map(
					"limit" -> c
				)
			)
		)
	}

	def configItemDataSize(s: String):
	Map[String, Any] = {
		Map(
			"item" -> Map(
				"data" -> Map(
					"size" -> s
				)
			)
		)
	}

	def configCreate(
		iterId: String, size: String, itemOutputFile: String, limitCount: Long, limitTime: Int
	):
	Map[String, Any] = {
		Map(
			"item" -> Map(
				"data" -> Map(
					"size" -> size
				),
				"output" -> Map(
					"file" -> itemOutputFile,
					"path" -> "/default"
				)
			),
			"load" -> Map(
				"step" -> Map(
					"id" -> ("create0_" + iterId),
					"limit" -> Map(
						"count" -> limitCount,
						"time" -> limitTime.toString
					)
				)
			)
		)
	}

	def configRead(iterId: String, itemInputFile: String):
	Map[String, Any] = {
		Map(
			"item" -> Map(
				"input" -> Map(
				"file" -> itemInputFile
				)
			),
			"load" -> Map(
				"step" -> Map(
					"id" -> ("read1_" + iterId)
				)
			)
		)
	}

	def configUpdate(iterId: String, itemInputFile: String, itemOutputFile: String):
	Map[String, Any] = Map(
		"item" -> Map(
			"input" -> Map(
				"file" -> itemInputFile
			),
			"output" -> Map(
				"file" -> itemOutputFile
			)
		),
		"load" -> Map(
			"step" -> Map(
				"id" -> ("update2_" + iterId)
			)
		)
	)

	def configReadPartial(iterId: String, itemInputFile: String):
	Map[String, Any] = Map(
		"item" -> Map(
			"input" -> Map(
				"file" -> itemInputFile
			)
		),
		"load" -> Map(
			"step" -> Map(
				"id" -> ("read3_" + iterId)
			)
		)
	)

	def configDelete(iterId: String, itemInputFile: String):
	Map[String, Any] = Map(
		"item" -> Map(
			"input" -> Map(
				"file" -> itemInputFile
			)
		),
		"load" -> Map(
			"step" -> Map(
				"id" -> ("delete4_" + iterId)
			)
		)
	)

	def run: Unit = {

		// typically OS open files limit is 1024 so won't try to use the concurrency level higher than that
		for(limitConcurrency <- Iterator.iterate(1)(_ * limitConcurrencyFactor) takeWhile (_ < 1024)) {

			for(i <- itemDataSizes.indices) {

				val itemDataSize = itemDataSizes(i)
				System.out.println(
					"Run the load steps using the concurrency limit of " + limitConcurrency
						+ " and items data size " + itemDataSize
				)
				val nextConfigLimitConcurrency = configLimitConcurrency(limitConcurrency)
				val iterId = "concurrency" + limitConcurrency + "_size" + itemDataSize
				val iterItemsFile0 = "items_" + iterId + "_0.csv"
				val iterItemsFile11 = "items_" + iterId + "_1.csv"

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
		}
	}
}
