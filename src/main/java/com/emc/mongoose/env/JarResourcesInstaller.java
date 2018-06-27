package com.emc.mongoose.env;

import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;

import org.apache.logging.log4j.Level;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public abstract class JarResourcesInstaller
implements Installer {

	@Override
	public void accept(final Path appHomePath) {
		try {
			Files.createDirectories(appHomePath);
		} catch(final IOException e) {
			e.printStackTrace(System.err);
		}
		resourceFilesToInstall().forEach(resFile -> installResourcesFile(appHomePath, resFile));
		Loggers.MSG.debug("Installer finished: \"{}\"", getClass().getCanonicalName());
	}

	private void installResourcesFile(final Path appHomePath, final String srcFilePath) {

		final Path dstPath = Paths.get(appHomePath.toString(), srcFilePath);

		if(dstPath.toFile().exists()) {
			Loggers.MSG.debug("The file {} already exists, checking the checksum", dstPath);

			final MessageDigest md;
			try {
				md = MessageDigest.getInstance("MD5");
			} catch(final NoSuchAlgorithmException e) {
				throw new AssertionError(e);
			}

			try(
				final InputStream in = resourceStream(srcFilePath);
				final DigestInputStream din = new DigestInputStream(in, md)
			) {
				while(-1 < din.read());
			} catch(final EOFException ok) {
			} catch(final IOException e) {
				LogUtil.exception(Level.WARN, e, "Failed to read the src file \"{}\"", srcFilePath);
			}
			final byte[] srcFileChecksum = md.digest();

			md.reset();

			try(
				final InputStream in = Files.newInputStream(dstPath, StandardOpenOption.READ);
				final DigestInputStream din = new DigestInputStream(in, md)
			) {
				while(-1 < din.read());
			} catch(final EOFException ok) {
			} catch(final IOException e) {
				LogUtil.exception(Level.WARN, e, "Failed to read the dst file \"{}\"", dstPath);
			}
			final byte[] dstFileChecksum = md.digest();

			final Base64.Encoder base64enc = Base64.getEncoder();
			if(Arrays.equals(srcFileChecksum, dstFileChecksum)) {
				Loggers.MSG.debug(
					"The destination file \"{}\" has the same checksum ({}) as source, skipping", dstPath,
					base64enc.encodeToString(dstFileChecksum)
				);
				return;
			} else {
				Loggers.MSG.info(
					"The destination file \"{}\" has the different checksum ({}) than source ({}), replacing", dstPath,
					base64enc.encodeToString(dstFileChecksum), base64enc.encodeToString(srcFileChecksum)
				);
				try {
					Files.delete(dstPath);
				} catch(final IOException e) {
					LogUtil.exception(Level.WARN, e, "Failed to remove the outdated file \"{}\"", dstPath);
				}
			}

		} else {
			dstPath.getParent().toFile().mkdirs();
		}

		try(final InputStream srcFileInput = resourceStream(srcFilePath)) {
			final long copiedBytesCount = Files.copy(srcFileInput, dstPath);
			Loggers.MSG.debug("The file {} installed ({})", dstPath, copiedBytesCount);
		} catch(final IOException e) {
			LogUtil.exception(Level.WARN, e, "Failed to install file {}", dstPath);
		}
	}

	protected InputStream resourceStream(final String resPath) {
		return getClass().getResourceAsStream("/" + resPath);
	}

	protected abstract List<String> resourceFilesToInstall();
}
