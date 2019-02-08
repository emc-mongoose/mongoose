package com.emc.mongoose.storage.driver.coop.jep321;

import static com.emc.mongoose.base.Constants.APP_NAME;

import com.emc.mongoose.base.data.DataInput;
import com.emc.mongoose.base.env.ExtensionBase;
import com.emc.mongoose.base.exception.OmgShootMyFootException;
import com.emc.mongoose.base.item.Item;
import com.emc.mongoose.base.item.op.Operation;
import com.emc.mongoose.base.storage.driver.StorageDriverFactory;
import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.SchemaProvider;
import com.github.akurilov.confuse.io.json.JsonSchemaProviderBase;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Jep321StorageDriverExtension<
        I extends Item, O extends Operation<I>, T extends Jep321StorageDriverBase<I, O>>
    extends ExtensionBase implements StorageDriverFactory<I, O, T> {

  private static final SchemaProvider SCHEMA_PROVIDER =
      new JsonSchemaProviderBase() {

        @Override
        protected final InputStream schemaInputStream() {
          return getClass().getResourceAsStream("/config-schema-storage-jep321.json");
        }

        @Override
        public final String id() {
          return APP_NAME;
        }
      };

  private static final String DEFAULTS_FILE_NAME = "defaults-storage-jep321.json";

  private static final List<String> RES_INSTALL_FILES =
      Collections.unmodifiableList(Arrays.asList("config/" + DEFAULTS_FILE_NAME));

  @Override
  public final SchemaProvider schemaProvider() {
    return SCHEMA_PROVIDER;
  }

  @Override
  protected final String defaultsFileName() {
    return DEFAULTS_FILE_NAME;
  }

  @Override
  public String id() {
    return "jep321";
  }

  @Override
  protected final List<String> resourceFilesToInstall() {
    return RES_INSTALL_FILES;
  }

  @Override
  public T create(
      final String stepId,
      final DataInput dataInput,
      final Config storageConfig,
      final boolean verifyFlag,
      final int batchSize)
      throws OmgShootMyFootException, InterruptedException {
    return (T)
        new Jep321StorageDriverBase<I, O>(stepId, dataInput, storageConfig, verifyFlag, batchSize);
  }
}
