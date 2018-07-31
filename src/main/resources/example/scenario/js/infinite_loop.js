while(true) {
	Load
		.config(
			{
				"load": {
					"step": {
						"limit": {
							"time": 1
						}
					}
				}
			}
		)
		.run();
}
