while(true) {
	Load
		.config(
			{
				"load": {
					"step": {
						"limit": {
							"count" : 1000
						}
					}
				}
			}
		)
		.run();
}