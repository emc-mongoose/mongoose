import com.emc.mongoose.env.Extension;
import com.emc.mongoose.load.step.weighted.WeightedLoadStepExtension;

module com.emc.mongoose.load.step.weighted {
  requires com.emc.mongoose;
  requires confuse;
  requires confuse.io.json;
  requires fiber4j;
  requires java.commons;
  requires log4j.api;
  requires java.base;

  exports com.emc.mongoose.load.step.weighted;

  provides Extension with
      WeightedLoadStepExtension;
}
