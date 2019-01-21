import com.emc.mongoose.env.Extension;
import com.emc.mongoose.load.step.linear.LinearLoadStepExtension;

module com.emc.mongoose.load.step.linear {
  requires com.emc.mongoose;
  requires confuse;
  requires confuse.io.json;
  requires fiber4j;
  requires java.commons;
  requires log4j.api;
  requires java.base;

  exports com.emc.mongoose.load.step.linear;

  provides Extension with
      LinearLoadStepExtension;
}
