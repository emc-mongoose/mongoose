package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.api.model.item.ItemNamingType;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.item.ItemConfig;
import com.emc.mongoose.ui.config.item.input.InputConfig;
import com.emc.mongoose.ui.config.item.naming.NamingConfig;

import java.util.List;

public interface StepSliceUtil {

	static void sliceConfig(
		final Config parent, final List<String> nodeAddrs, final List<Config> configSlices
	) {

		final ItemConfig itemConfig = parent.getItemConfig();
		final InputConfig itemInputConfig = itemConfig.getInputConfig();
		/* TODO
		Slice the items input if necessary stream the items from the input to the item
		input files located on the remote nodes. Then set the corresponding item input
		file name to the config slice.
		 */

		nodeAddrs
			.stream()
			.forEach(
				n -> {

					final Config configSlice = new Config(parent);
					configSlices.add(configSlice);

					final NamingConfig itemNamingConfig = itemConfig.getNamingConfig();
					final ItemNamingType itemNamingType = ItemNamingType.valueOf(
						itemNamingConfig.getType().toLowerCase()
					);
					if()
				}
			);
	}
}
