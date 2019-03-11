PipelineLoad
	.append({
	"item" : {
		"output" : {
		"delay" : "20s"
		}
	},
	"storage" : {
		"net" : {
		"node" : {
			"addrs" : ZONE1_ADDRS
		}
		}
	}
	})
	.append({
	"load" : {
		"op" : {
		"type" : "read"
		}
	},
	"storage" : {
		"net" : {
		"node" : {
			"addrs" : ZONE2_ADDRS
		}
		}
	}
	})
	.run();
