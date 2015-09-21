package com.emc.mongoose.run.cli;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;
//
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
/**
 * Date:   12/10/14
 * Time:   4:37 PM
 */
public class HumanFriendly {

    public enum CLIOption {

        IP("i", "Comma-separated list of ip addresses to write to", true, RunTimeConfig.KEY_STORAGE_ADDRS),
        USER("u", "User", true, RunTimeConfig.KEY_AUTH_ID),
        SECRET("s", "Secret", true, RunTimeConfig.KEY_AUTH_SECRET),
        BUCKET("b","Bucket to write data to", true, RunTimeConfig.KEY_API_S3_BUCKET),
        READ("r", "Perform object read", true, new CompositeOptionConverter(RunTimeConfig.KEY_SCENARIO_SINGLE_LOAD,
                "read", RunTimeConfig.KEY_DATA_SRC_FPATH)),
        WRITE("w", "Perform object write", false, new CompositeOptionConverter(RunTimeConfig.KEY_SCENARIO_SINGLE_LOAD, "create")),
        DELETE("d", "Perform object delete", false, new CompositeOptionConverter(RunTimeConfig.KEY_SCENARIO_SINGLE_LOAD, "delete",
                RunTimeConfig.KEY_DATA_SRC_FPATH)),
        LENGTH("l", "Size of the object to write", true, RunTimeConfig.KEY_DATA_SIZE_MIN, RunTimeConfig.KEY_DATA_SIZE_MAX),
        COUNT("c", "Count of objects to write", true, RunTimeConfig.KEY_DATA_ITEM_COUNT),
        CONNS("n", "Number of concurrent connections per storage node", true,
	        RunTimeConfig.getLoadConcurrencyParamName("create"),
	        RunTimeConfig.getLoadConcurrencyParamName("read"),
	        RunTimeConfig.getLoadConcurrencyParamName("update"),
	        RunTimeConfig.getLoadConcurrencyParamName("delete"),
	        RunTimeConfig.getLoadConcurrencyParamName("append")
        ),
        THREADS("t", "Number of I/O threads", true, RunTimeConfig.getLoadWorkersParamName("create"),
                RunTimeConfig.getLoadWorkersParamName("update"),
                RunTimeConfig.getLoadWorkersParamName("delete"),
                RunTimeConfig.getLoadWorkersParamName("read"),
	            RunTimeConfig.getLoadWorkersParamName("append")
        ),
        HELP("h", "Displays this message", false, new NullOptionConverter()),
        RUN_ID("z", "Sets run id", true, new SystemOptionConverter(RunTimeConfig.KEY_RUN_ID)),
        USE_DEPLOYMENT_OUTPUT("o", "Use deployment output", false, new DeploymentOutputConverter());

        private final String shortName;
        private final String description;
        private final boolean hasArg;
        private final OptionConverter optionConverter;

        private CLIOption(String shortName, String description, boolean hasArg, OptionConverter converter) {
            this.shortName = shortName;
            this.description = description;
            this.hasArg = hasArg;
            this.optionConverter = converter;
        }

        private CLIOption(String shortName, String description, boolean hasArg, String ... commonPropertyName) {
            this.shortName = shortName;
            this.description = description;
            this.hasArg = hasArg;
            this.optionConverter = new DefaultOptionConverter(commonPropertyName);
        }

        public OptionConverter converter(){
            return optionConverter;
        }

        public Option toOption() {
            return new Option(shortName, this.name().toLowerCase().replace('_', '-'), hasArg, description);
        }

        public static CLIOption fromOption(Option option){
            return CLIOption.valueOf(option.getLongOpt().toUpperCase().replace('-', '_'));
        }
    }

    public static void main(String[] args){
        System.out.println(parseCli(args));
    }

    public static Map<String, String> parseCli(String[] args) {

        Options options = new Options();

        for(CLIOption opt : CLIOption.values()) {
            options.addOption(opt.toOption());
        }

        try {
            CommandLineParser commandLineParser = new GnuParser();
            CommandLine cmdLine = commandLineParser.parse(options, args);

            if(cmdLine.hasOption(CLIOption.HELP.toString().toLowerCase())){
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("Mongoose", options);
                System.exit(0);
            }

            Map<String, String> values = new HashMap<>();

            for(Option option : cmdLine.getOptions()) {
                values.putAll(CLIOption.fromOption(option)
                        .converter().convertOption(option.getValue()));
            }

            return values;
        } catch(ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Mongoose", options);
            System.exit(1);
        } catch(Exception e) {
            e.printStackTrace(System.err);
            System.exit(0);
        }

        return null;
    }

    private static interface OptionConverter{

        public Map<String, String> convertOption(String value) throws Exception;

    }

    private static class DefaultOptionConverter implements OptionConverter{

        private final String[] commonPropertyNames;

        public DefaultOptionConverter(String ... commonPropertyNames) {
            this.commonPropertyNames = commonPropertyNames;
        }

        @Override
        public Map<String, String> convertOption(String value) {

            Map<String, String> result = new HashMap<>();
            for(String property : commonPropertyNames){
                result.put(property, value);
            }

            return result;
        }
    }

    private static class CompositeOptionConverter implements OptionConverter {

        private final String key;

        private final String staticValue;

        private final String[] configuredValueKeys;

        public CompositeOptionConverter(String key, String staticValue, String ... configuredValueKeys) {
            this.key = key;
            this.staticValue = staticValue;
            this.configuredValueKeys = configuredValueKeys;
        }

        @Override
        public Map<String, String> convertOption(String value) throws Exception {
            Map<String, String> result = new HashMap<>();

            result.put(key, staticValue);

            for(String configuredKey : configuredValueKeys){
                result.put(configuredKey, value);
            }

            return result;
        }
    }

    private static class DeploymentOutputConverter implements OptionConverter{

        @Override
        public Map<String, String> convertOption(String value) throws Exception {

            File file;

            String fileName = System.getenv("DevBranch");

            file = new File(fileName + "/tools/cli/python/DeploymentOutput");
            if(!file.exists()){
                file = new File(fileName + "/tools/cli/python/StandaloneDeploymentOutput");
            }

            Properties props = new Properties();

            try(FileInputStream stream = new FileInputStream(file)){
                props.load(stream);
            }

            Map<String, String> result = new HashMap<>();

            result.put(RunTimeConfig.KEY_AUTH_ID, props.getProperty("user"));
            result.put(RunTimeConfig.KEY_AUTH_SECRET, props.getProperty("secretkey"));
            result.put(RunTimeConfig.KEY_API_S3_BUCKET, props.getProperty("bucket").split(" ")[0]);

            String dataNodes = System.getenv("DataNodes")
                    .replace('(', ' ').replace(')', ' ').trim().replace(' ', ',');

            String s3Ports = props.getProperty("s3UnSecurePort");

            if(s3Ports != null){
                //Looks like we are working with StandaloneDeploymentOutput with custom port config
                String firstDataNode = dataNodes.split(",")[0];

                List<String> address = new ArrayList<>();

                for(String port : s3Ports.split(",")){
                    address.add(firstDataNode + ":" + port);
                }

                result.put(RunTimeConfig.KEY_STORAGE_ADDRS, StringUtils.join(address, ','));
            }else{
               result.put(RunTimeConfig.KEY_STORAGE_ADDRS, dataNodes);
            }

            return result;
        }
    }

    private static class NullOptionConverter implements OptionConverter{

        @Override
        public Map<String, String> convertOption(String value) throws Exception {
            return Collections.emptyMap();
        }
    }

    private static class SystemOptionConverter implements OptionConverter{

        private final String systemPropertyKey;

        public SystemOptionConverter(String systemProperty) {
            this.systemPropertyKey = systemProperty;
        }

        @Override
        public Map<String, String> convertOption(String value) throws Exception {
            System.setProperty(systemPropertyKey, value);
            return Collections.singletonMap(systemPropertyKey, value);
        }
    }

}
