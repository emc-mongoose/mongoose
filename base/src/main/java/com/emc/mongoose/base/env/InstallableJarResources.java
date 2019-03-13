package com.emc.mongoose.base.env;

import com.emc.mongoose.base.logging.LogUtil;
import com.emc.mongoose.base.logging.Loggers;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.zip.Adler32;
import java.util.zip.Checksum;
import org.apache.logging.log4j.Level;

public abstract class InstallableJarResources implements Installable {

	@Override
	public void install(final Path dstPath) {
		try {
			Files.createDirectories(dstPath);
		} catch (final IOException e) {
			LogUtil.trace(
							Loggers.ERR, Level.ERROR, e, "Failed to create directories in the path to {}", dstPath);
		}
		resourceFilesToInstall().forEach(resFile -> installResourcesFile(dstPath, resFile));
		Loggers.MSG.debug("Installer finished: \"{}\"", getClass().getCanonicalName());
	}

	private void installResourcesFile(final Path appHomePath, final String srcFilePath) {

		final var dstPath = Paths.get(appHomePath.toString(), srcFilePath);

		if (dstPath.toFile().exists()) {
			Loggers.MSG.debug("The file {} already exists, checking the checksum", dstPath);

			final Checksum checksumCalc = new Adler32();
			final var buff = new byte[0x2000];
			int n;

			try (final var in = resourceStream(srcFilePath)) {
				if (in == null) {
					Loggers.ERR.warn("Resource file \"{}\" not found, skipping", srcFilePath);
				} else {
					while (-1 < (n = in.read(buff))) {
						checksumCalc.update(buff, 0, n);
					}
				}
			} catch (final EOFException ok) {} catch (final IOException e) {
				LogUtil.exception(Level.WARN, e, "Failed to read the src file \"{}\"", srcFilePath);
			}
			final var srcFileChecksum = checksumCalc.getValue();

			checksumCalc.reset();

			try (final var in = Files.newInputStream(dstPath, StandardOpenOption.READ)) {
				while (-1 < (n = in.read(buff))) {
					checksumCalc.update(buff, 0, n);
				}
			} catch (final EOFException ok) {} catch (final IOException e) {
				LogUtil.exception(Level.WARN, e, "Failed to read the dst file \"{}\"", dstPath);
			}
			final var dstFileChecksum = checksumCalc.getValue();

			if (srcFileChecksum == dstFileChecksum) {
				Loggers.MSG.debug(
								"The destination file \"{}\" has the same checksum ({}) as source, skipping",
								dstPath,
								Long.toHexString(srcFileChecksum));
				return;
			} else {
				Loggers.MSG.warn(
								"The destination file \"{}\" has the different checksum ({}) than source ({}), replacing",
								dstPath,
								Long.toHexString(dstFileChecksum),
								Long.toHexString(srcFileChecksum));
				try {
					Files.delete(dstPath);
				} catch (final IOException e) {
					LogUtil.exception(Level.WARN, e, "Failed to remove the outdated file \"{}\"", dstPath);
				}
			}

		} else {
			dstPath.getParent().toFile().mkdirs();
		}

		try (final var srcFileInput = resourceStream(srcFilePath)) {
			final var copiedBytesCount = Files.copy(srcFileInput, dstPath);
			Loggers.MSG.debug("The file {} installed ({})", dstPath, copiedBytesCount);
		} catch (final Exception e) {
			LogUtil.exception(Level.WARN, e, "Failed to copy file from {} to {}", srcFilePath, dstPath);
		}
	}

	protected InputStream resourceStream(final String resPath) {
		return getClass().getResourceAsStream("/" + resPath);
	}

	protected abstract List<String> resourceFilesToInstall();
}
