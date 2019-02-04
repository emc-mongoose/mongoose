package com.emc.mongoose.env;

import com.emc.mongoose.logging.Loggers;
import java.io.File;
import java.nio.file.Path;

public interface FsUtil {

  static void createParentDirsIfNotExist(final Path path) {
    if (path != null) {
      final Path parentDirPath = path.getParent();
      if (parentDirPath != null) {
        final File parentDir = parentDirPath.toFile();
        if (!parentDir.exists() && !parentDir.mkdirs()) {
          Loggers.ERR.warn(
              "Failed to create parent directories for the item info output file \"{}\"", path);
        }
      }
    }
  }
}
