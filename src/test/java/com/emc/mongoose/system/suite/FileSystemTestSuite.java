package com.emc.mongoose.system.suite;

import com.emc.mongoose.system.feature.filesystem.CircularReadFromCustomDirTest;
import com.emc.mongoose.system.feature.filesystem.OverwriteCircularlyTest;
import com.emc.mongoose.system.feature.filesystem.ReadDirsWithFilesTest;
import com.emc.mongoose.system.feature.filesystem.ReadFromCustomDirTest;
import com.emc.mongoose.system.feature.filesystem.UpdateAndVerifyTest;
import com.emc.mongoose.system.feature.filesystem.WriteDirsToCustomDirTest;
import com.emc.mongoose.system.feature.filesystem.WriteToCustomDirTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 Created by kurila on 23.05.16.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	CircularReadFromCustomDirTest.class,
	OverwriteCircularlyTest.class,
	/*ReadDirsWithFilesTest.class,
	ReadFromCustomDirTest.class,
	UpdateAndVerifyTest.class,
	WriteDirsToCustomDirTest.class,
	WriteToCustomDirTest.class,*/
})
public class FileSystemTestSuite {
	// files
	// directories
}
