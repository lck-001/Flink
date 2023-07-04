package httputils;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.format.DecodingFormat;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.connector.source.ScanTableSource;
import org.apache.flink.table.connector.source.SourceFunctionProvider;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;

public class HttpDynamicTableSource implements ScanTableSource {

    private final String url;
    private final String appId;
    private final String appSecret;
    private final String appToken;
    private final String tableId;
    private final String viewId;
    private final String httpFilter;
    private final long interval;
    private final DecodingFormat<DeserializationSchema<RowData>> decodingFormat;
    private final DataType producedDataType;

    public HttpDynamicTableSource(
            String hostname,
            String appId,
            String appSecret,
            String appToken,
            String tableId,
            String viewId,
            String httpFilter,
            long interval,
            DecodingFormat<DeserializationSchema<RowData>> decodingFormat,
            DataType producedDataType) {
        this.url = hostname;
        this.appId=appId;
        this.appSecret=appSecret;
        this.appToken=appToken;
        this.tableId=tableId;
        this.viewId=viewId;
        this.httpFilter=httpFilter;
        this.interval = interval;
        this.decodingFormat = decodingFormat;
        this.producedDataType = producedDataType;
    }

    @Override
    public ChangelogMode getChangelogMode() {
        // in our example the format decides about the changelog mode
        // but it could also be the source itself
        return decodingFormat.getChangelogMode();
    }

    @Override
    public ScanRuntimeProvider getScanRuntimeProvider(ScanContext runtimeProviderContext) {

        // create runtime classes that are shipped to the cluster
        final DeserializationSchema<RowData> deserializer = decodingFormat.createRuntimeDecoder(
                runtimeProviderContext,
                producedDataType);

        final SourceFunction<RowData> sourceFunction = new HttpSource(url,appId ,appSecret,appToken,tableId,viewId,httpFilter, interval, deserializer);

        return SourceFunctionProvider.of(sourceFunction, false);
    }

    @Override
    public DynamicTableSource copy() {
        return new HttpDynamicTableSource(url,appId ,appSecret,appToken,tableId,viewId,httpFilter, interval, decodingFormat, producedDataType);
    }

    @Override
    public String asSummaryString() {
        return "Http Table Source";
    }
}