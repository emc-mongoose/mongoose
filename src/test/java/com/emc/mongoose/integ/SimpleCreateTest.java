package com.emc.mongoose.integ;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.run.scenario.ScriptRunner;
//
import com.emc.mongoose.storage.mock.impl.Cinderella;
//
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
//
import java.io.IOException;
import java.nio.file.Paths;
/**
 * Created by olga on 30.06.15.
 */
public class SimpleCreateTest {

	private RunTimeConfig runTimeConfig;

	@Before
	public void runCinderella(){
		//
		LogUtil.init();
		RunTimeConfig.initContext();
		runTimeConfig = RunTimeConfig.getContext();
		runTimeConfig.loadPropsFromJsonCfgFile(
			Paths.get(RunTimeConfig.DIR_ROOT, Constants.DIR_CONF)
				.resolve(RunTimeConfig.FNAME_CONF)
		);
		//
		final Thread wsMockThread;
		try {
			wsMockThread = new Thread(
				new Cinderella<>(RunTimeConfig.getContext()), "wsMock"
			);
			wsMockThread.setDaemon(true);
			wsMockThread.start();
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}

		//
	}

	@Test
	public void writeDefaultScenarioTest(){
		runTimeConfig.set("load.limit.count", 10);
		new ScriptRunner().run();
		//

	}

	@Test
	public void simpleTest(){
		Assert.assertEquals(1,2);
		//

	}

}
