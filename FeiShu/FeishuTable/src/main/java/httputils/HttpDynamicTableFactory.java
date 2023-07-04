package httputils;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.table.connector.format.DecodingFormat;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.factories.DeserializationFormatFactory;
import org.apache.flink.table.factories.DynamicTableSourceFactory;
import org.apache.flink.table.factories.FactoryUtil;
import org.apache.flink.table.types.DataType;

import java.util.HashSet;
import java.util.Set;

public class HttpDynamicTableFactory implements DynamicTableSourceFactory {

    // define all options statically
    public static final ConfigOption<String> URL = ConfigOptions.key("http.url")
            .stringType()
            .noDefaultValue();

    public static final ConfigOption<Long> INTERVAL = ConfigOptions.key("http.interval")
            .longType()
            .noDefaultValue();

    public static final ConfigOption<String> APP_ID = ConfigOptions.key("http.appId")
            .stringType()
            .noDefaultValue();

    public static final ConfigOption<String> APP_SECRET = ConfigOptions.key("http.appSecret")
            .stringType()
            .noDefaultValue();


    public static final ConfigOption<String> APP_TOKEN = ConfigOptions.key("http.appToken")
            .stringType()
            .noDefaultValue();

    public static final ConfigOption<String> TABLE_ID = ConfigOptions.key("http.tableId")
            .stringType()
            .noDefaultValue();

    public static final ConfigOption<String> VIEW_ID = ConfigOptions.key("http.viewId")
            .stringType()
            .noDefaultValue();

    public static final ConfigOption<String> HTTP_FILTER = ConfigOptions.key("http.httpFilter")
            .stringType()
            .noDefaultValue();

    @Override
    public String factoryIdentifier() {
        return "http"; // used for matching to `connector = '...'`
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        final Set<ConfigOption<?>> options = new HashSet<>();
        options.add(URL);
        options.add(INTERVAL);
        options.add(APP_ID);
        options.add(APP_SECRET);
        options.add(APP_TOKEN);
        options.add(TABLE_ID);
        options.add(VIEW_ID);
        options.add(HTTP_FILTER);
        options.add(FactoryUtil.FORMAT); // use pre-defined option for format
        return options;
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        final Set<ConfigOption<?>> options = new HashSet<>();
        // no optional option
//        options.add(BYTE_DELIMITER);
        return options;
    }

    @Override
    public DynamicTableSource createDynamicTableSource(Context context) {
        // either implement your custom validation logic here ...
        // or use the provided helper utility
        final FactoryUtil.TableFactoryHelper helper = FactoryUtil.createTableFactoryHelper(this, context);

        // discover a suitable decoding format
        final DecodingFormat<DeserializationSchema<RowData>> decodingFormat = helper.discoverDecodingFormat(
                DeserializationFormatFactory.class,
                FactoryUtil.FORMAT);

        // validate all options
        helper.validate();

        // get the validated options
        final ReadableConfig options = helper.getOptions();
        final String url = options.get(URL);
        final String appId = options.get(APP_ID);
        final String appSecret = options.get(APP_SECRET);
        final String appToken = options.get(APP_TOKEN);
        final String tableId = options.get(TABLE_ID);
        final String viewId = options.get(VIEW_ID);
        final String httpFilter = options.get(HTTP_FILTER);
        final long interval = options.get(INTERVAL);

        // derive the produced data type (excluding computed columns) from the catalog table
        final DataType producedDataType =
                context.getCatalogTable().getResolvedSchema().toPhysicalRowDataType();

        // create and return dynamic table source
        return new HttpDynamicTableSource(url,appId ,appSecret,appToken,tableId,viewId,httpFilter,interval, decodingFormat, producedDataType);
    }
}
