package com.emc.mongoose.integ.suite;
/**
 Created by kurila on 02.12.15.
 */
import com.emc.mongoose.integ.feature.filesystem.AppendTest;
import com.emc.mongoose.integ.feature.filesystem.CircularReadFromCustomDirTest;
import com.emc.mongoose.integ.feature.filesystem.FileRampupTest;
import com.emc.mongoose.integ.feature.filesystem.OverwriteCircularlyTest;
import com.emc.mongoose.integ.feature.filesystem.ReadDirsWithFilesTest;
import com.emc.mongoose.integ.feature.filesystem.ReadFromCustomDirTest;
import com.emc.mongoose.integ.feature.filesystem.UpdateAndVerifyTest;
import com.emc.mongoose.integ.feature.filesystem.WRADParallelLoadTest;
import com.emc.mongoose.integ.feature.filesystem.WURDSequentialLoadTest;
import com.emc.mongoose.integ.feature.filesystem.WriteDirsToCustomDirTest;
import com.emc.mongoose.integ.feature.filesystem.WriteToCustomDirTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
@RunWith(Suite.class)
@Suite.SuiteClasses({
	WriteToCustomDirTest.class,
	ReadFromCustomDirTest.class,
	OverwriteCircularlyTest.class,
	AppendTest.class,
	CircularReadFromCustomDirTest.class,
	UpdateAndVerifyTest.class,
	WRADParallelLoadTest.class,
	WURDSequentialLoadTest.class,
	FileRampupTest.class,
	WriteDirsToCustomDirTest.class,
	ReadDirsWithFilesTest.class,
})
public class FileSystemTestSuite {
}
