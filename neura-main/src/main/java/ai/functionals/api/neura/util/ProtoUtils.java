package ai.functionals.api.neura.util;

import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProtoUtils {
    private ProtoUtils() { }

    public static String protobufPrint(MessageOrBuilder message) {
        try {
            return JsonFormat.printer().omittingInsignificantWhitespace().print(message);
        } catch (Exception e) {
            log.error("Error while converting protobuf to json", e);
            return "{}";
        }
    }

    public static MessageOrBuilder protobufParse(String json, Message.Builder builder) {
        try {
            JsonFormat.parser().merge(json, builder);
            return builder.build();
        } catch (Exception e) {
            log.error("Error while converting json to protobuf", e);
            return null;
        }
    }
}
