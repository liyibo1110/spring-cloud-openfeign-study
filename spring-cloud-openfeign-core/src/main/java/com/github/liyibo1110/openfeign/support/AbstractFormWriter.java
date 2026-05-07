package com.github.liyibo1110.openfeign.support;

import feign.codec.EncodeException;
import feign.form.ContentProcessor;
import feign.form.multipart.AbstractWriter;
import feign.form.multipart.Output;
import feign.form.util.PojoUtil;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.function.Predicate;

/**
 * @author liyibo
 * @date 2026-05-06 14:08
 */
public abstract class AbstractFormWriter extends AbstractWriter {

    @Override
    public boolean isApplicable(Object object) {
        return !isTypeOrCollection(object, o -> o instanceof MultipartFile) && isTypeOrCollection(object, PojoUtil::isUserPojo);
    }

    @Override
    public void write(Output output, String key, Object object) throws EncodeException {
        try {
            String string = new StringBuilder().append("Content-Disposition: form-data; name=\"")
                    .append(key)
                    .append('"')
                    .append(ContentProcessor.CRLF)
                    .append("Content-Type: ")
                    .append(getContentType())
                    .append("; charset=")
                    .append(output.getCharset().name())
                    .append(ContentProcessor.CRLF)
                    .append(ContentProcessor.CRLF)
                    .append(writeAsString(object))
                    .toString();
            output.write(string);
        }
        catch (IOException e) {
            throw new EncodeException(e.getMessage());
        }
    }

    protected abstract MediaType getContentType();

    protected abstract String writeAsString(Object object) throws IOException;

    private boolean isTypeOrCollection(Object object, Predicate<Object> isType) {
        if (object == null)
            return false;

        if (object.getClass().isArray()) {
            int len = Array.getLength(object);
            if (len > 0) {
                Object one = Array.get(object, 0);
                return len > 1 && one != null && isType.test(one);
            }
            return false;
        } else if (object instanceof Iterable<?> iterable) {
            Iterator<?> iterator = iterable.iterator();
            return iterator.hasNext() && isType.test(iterator.next());
        } else {
            return isType.test(object);
        }
    }
}
