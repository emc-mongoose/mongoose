import com.emc.mongoose.base.env.Extension;
import com.emc.mongoose.load.step.linear.LinearLoadStepExtension;

module com.emc.mongoose.load.step.linear {
	requires com.emc.mongoose.base;
	requires com.github.akurilov.commons;
	requires com.github.akurilov.confuse;
	requires com.github.akurilov.confuse.io.json;
	requires com.github.akurilov.fiber4j;
	requires log4j.api;
	requires java.base;

	exports com.emc.mongoose.load.step.linear;

	provides Extension with
		LinearLoadStepExtension;
}
