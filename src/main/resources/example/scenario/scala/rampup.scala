import com.github.akurilov.commons.system.SizeInBytes

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

def configItemDataSize(s: SizeInBytes):
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
	iterId: String, size: SizeInBytes, itemOutputFile: String, limitCount: Long, limitTime: String
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
					"time" -> limitTime
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

for(limitConcurrency <- 1 until 1024) {

	limitConcurrency *= limitConcurrencyFactor
}
