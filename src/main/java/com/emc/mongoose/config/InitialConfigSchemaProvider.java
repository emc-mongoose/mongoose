package com.emc.mongoose.config;

import static com.emc.mongoose.Constants.APP_NAME;

import com.github.akurilov.confuse.SchemaProvider;
import com.github.akurilov.confuse.io.json.JsonSchemaProviderBase;
import java.io.IOException;
import java.io.InputStream;

public final class InitialConfigSchemaProvider extends JsonSchemaProviderBase {

  @Override
  public final String id() {
    return APP_NAME;
  }

  @Override
  protected final InputStream schemaInputStream() throws IOException {
    return getClass().getResource("/config-schema.json").openStream();
  }

  public static SchemaProvider provider() {
  	return new InitialConfigSchemaProvider();
  }
}
