/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.prestosql.decoder.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableSet;
import io.prestosql.decoder.DecoderColumnHandle;
import io.prestosql.decoder.FieldValueProvider;
import io.prestosql.spi.PrestoException;
import io.prestosql.spi.type.TimeZoneKey;
import io.prestosql.spi.type.Type;

import java.util.Set;

import static io.prestosql.decoder.DecoderErrorCode.DECODER_CONVERSION_NOT_SUPPORTED;
import static io.prestosql.decoder.json.JsonRowDecoderFactory.throwUnsupportedColumnType;
import static io.prestosql.spi.type.TimeType.TIME;
import static io.prestosql.spi.type.TimeWithTimeZoneType.TIME_WITH_TIME_ZONE;
import static io.prestosql.spi.type.TimestampType.TIMESTAMP;
import static io.prestosql.spi.type.TimestampWithTimeZoneType.TIMESTAMP_WITH_TIME_ZONE;
import static java.lang.Long.parseLong;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * Milliseconds since the epoch date format decoder.
 * <p>
 * Uses hardcoded UTC timezone and english locale.
 */
public class MillisecondsSinceEpochJsonFieldDecoder
        implements JsonFieldDecoder
{
    private static final Set<Type> SUPPORTED_TYPES = ImmutableSet.of(TIME, TIME_WITH_TIME_ZONE, TIMESTAMP, TIMESTAMP_WITH_TIME_ZONE);

    private final DecoderColumnHandle columnHandle;

    public MillisecondsSinceEpochJsonFieldDecoder(DecoderColumnHandle columnHandle)
    {
        this.columnHandle = requireNonNull(columnHandle, "columnHandle is null");
        if (!SUPPORTED_TYPES.contains(columnHandle.getType())) {
            throwUnsupportedColumnType(columnHandle);
        }
    }

    @Override
    public FieldValueProvider decode(JsonNode value)
    {
        return new MillisecondsSinceEpochJsonValueProvider(value, columnHandle);
    }

    public static class MillisecondsSinceEpochJsonValueProvider
            extends AbstractDateTimeJsonValueProvider
    {
        public MillisecondsSinceEpochJsonValueProvider(JsonNode value, DecoderColumnHandle columnHandle)
        {
            super(value, columnHandle);
        }

        @Override
        protected long getMillis()
        {
            if (value.isIntegralNumber() && !value.isBigInteger()) {
                return value.longValue();
            }
            if (value.isValueNode()) {
                try {
                    return parseLong(value.asText());
                }
                catch (NumberFormatException e) {
                    throw new PrestoException(
                            DECODER_CONVERSION_NOT_SUPPORTED,
                            format("could not parse value '%s' as '%s' for column '%s'", value.asText(), columnHandle.getType(), columnHandle.getName()));
                }
            }
            throw new PrestoException(
                    DECODER_CONVERSION_NOT_SUPPORTED,
                    format("could not parse non-value node as '%s' for column '%s'", columnHandle.getType(), columnHandle.getName()));
        }

        @Override
        protected TimeZoneKey getTimeZone()
        {
            return TimeZoneKey.UTC_KEY;
        }
    }
}
