package com.emc.mongoose.integ.suite;
/**
 Created by kurila on 02.12.15.
 */
import com.emc.mongoose.integ.feature.filesystem.OverwriteCircularlyTest;
import com.emc.mongoose.integ.feature.filesystem.ReadFromCustomDirTest;
import com.emc.mongoose.integ.feature.filesystem.WriteToCustomDirTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
@RunWith(Suite.class)
@Suite.SuiteClasses({
	WriteToCustomDirTest.class,
	ReadFromCustomDirTest.class,
	OverwriteCircularlyTest.class,
})
public class FileSystemTestSuite {
}
