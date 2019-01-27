import com.emc.mongoose.base.env.Extension;
import com.emc.mongoose.load.step.weighted.WeightedLoadStepExtension;

module com.emc.mongoose.load.step.weighted {
	requires com.emc.mongoose.base;
	requires com.github.akurilov.commons;
	requires com.github.akurilov.confuse;
	requires com.github.akurilov.confuse.io.json;
	requires com.github.akurilov.fiber4j;
	requires log4j.api;
	requires java.base;

	exports com.emc.mongoose.load.step.weighted;

	provides Extension with
		WeightedLoadStepExtension;
}
