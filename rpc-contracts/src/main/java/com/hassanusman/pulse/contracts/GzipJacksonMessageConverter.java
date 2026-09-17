package com.hassanusman.pulse.contracts;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Compresses JSON AMQP bodies above a byte threshold.
 * Small messages stay uncompressed to avoid gzip overhead on tiny RPCs.
 */
public class GzipJacksonMessageConverter implements MessageConverter {

    private final Jackson2JsonMessageConverter delegate;
    private final int thresholdBytes;
    private final Counter compressed;
    private final Counter passthrough;
    private final Counter bytesSaved;

    public GzipJacksonMessageConverter(ObjectMapper objectMapper, int thresholdBytes, MeterRegistry registry) {
        this.delegate = new Jackson2JsonMessageConverter(objectMapper);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("com.hassanusman.pulse.contracts");
        this.delegate.setJavaTypeMapper(typeMapper);
        this.thresholdBytes = thresholdBytes;
        MeterRegistry meters = registry == null ? new SimpleMeterRegistry() : registry;
        this.compressed = meters.counter("pulse.rpc.gzip.compressed");
        this.passthrough = meters.counter("pulse.rpc.gzip.passthrough");
        this.bytesSaved = meters.counter("pulse.rpc.gzip.bytes.saved");
    }

    public GzipJacksonMessageConverter(ObjectMapper objectMapper, int thresholdBytes) {
        this(objectMapper, thresholdBytes, new SimpleMeterRegistry());
    }

    @Override
    public Message toMessage(Object object, MessageProperties messageProperties) throws MessageConversionException {
        Message raw = delegate.toMessage(object, messageProperties);
        byte[] body = raw.getBody();
        if (body.length < thresholdBytes) {
            passthrough.increment();
            return raw;
        }
        byte[] gzipped = gzip(body);
        if (gzipped.length >= body.length) {
            passthrough.increment();
            return raw;
        }
        compressed.increment();
        bytesSaved.increment(body.length - gzipped.length);
        MessageProperties props = raw.getMessageProperties();
        props.setContentEncoding(RpcHeaders.COMPRESSED);
        props.setHeader(RpcHeaders.UNCOMPRESSED_SIZE, body.length);
        return new Message(gzipped, props);
    }

    @Override
    public Object fromMessage(Message message) throws MessageConversionException {
        if (RpcHeaders.COMPRESSED.equalsIgnoreCase(message.getMessageProperties().getContentEncoding())) {
            byte[] unzipped = gunzip(message.getBody());
            MessageProperties copy = new MessageProperties();
            copy.setHeaders(message.getMessageProperties().getHeaders());
            copy.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            copy.setContentEncoding(null);
            return delegate.fromMessage(new Message(unzipped, copy));
        }
        return delegate.fromMessage(message);
    }

    static byte[] gzip(byte[] input) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length / 2);
             GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(input);
            gzip.finish();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new MessageConversionException("Failed to gzip AMQP payload", e);
        }
    }

    static byte[] gunzip(byte[] input) {
        try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(input));
             ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length * 2)) {
            gis.transferTo(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new MessageConversionException("Failed to gunzip AMQP payload", e);
        }
    }
}
