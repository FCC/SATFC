package ca.ubc.cs.beta.stationpacking.webapp.filters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.util.zip.GZIPInputStream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.http.protocol.HTTP;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by newmanne on 27/10/15.
 * see http://stackoverflow.com/questions/20507007/http-request-compression
 */
@Slf4j
public class GzipRequestFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        if (req instanceof HttpServletRequest) {
            HttpServletRequest request = (HttpServletRequest) req;
            final String contentEncoding = request.getHeader(HTTP.CONTENT_ENCODING);
            if (contentEncoding != null && contentEncoding.contains("gzip")) {
                log.trace("gzip request detected");
                try {
                    final InputStream decompressStream = decompressStream(request.getInputStream());
                    req = new HttpServletRequestWrapper(request) {

                        @Override
                        public ServletInputStream getInputStream() throws IOException {
                            return new DecompressServletInputStream(decompressStream);
                        }

                        @Override
                        public BufferedReader getReader() throws IOException {
                            return new BufferedReader(new InputStreamReader(decompressStream));
                        }
                    };
                } catch (IOException e) {
                    log.error("Error while handling a gzip request", e);
                }

            }
        }
        chain.doFilter(req, res);
    }

    @Override
    public void destroy() {

    }


    public static class DecompressServletInputStream extends ServletInputStream {
        private InputStream inputStream;

        public DecompressServletInputStream(InputStream input) {
            inputStream = input;

        }

        @Override
        public int read() throws IOException {
            return inputStream.read();
        }

        @Override
        public boolean isFinished() {
            try {
                return inputStream.available() == 0;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Gzip magic number, fixed values in the beginning to identify the gzip
     * format <br>
     * http://www.gzip.org/zlib/rfc-gzip.html#file-format
     */
    private static final byte GZIP_ID1 = 0x1f;
    /**
     * Gzip magic number, fixed values in the beginning to identify the gzip
     * format <br>
     * http://www.gzip.org/zlib/rfc-gzip.html#file-format
     */
    private static final byte GZIP_ID2 = (byte) 0x8b;

    /**
     * Return decompression input stream if needed.
     *
     * @param input original stream
     * @return decompression stream
     * @throws IOException exception while reading the input
     */
    public static InputStream decompressStream(InputStream input) throws IOException {
        PushbackInputStream pushbackInput = new PushbackInputStream(input, 2);

        byte[] signature = new byte[2];
        pushbackInput.read(signature);
        pushbackInput.unread(signature);

        if (signature[0] == GZIP_ID1 && signature[1] == GZIP_ID2) {
            return new GZIPInputStream(pushbackInput);
        }
        return pushbackInput;
    }

}
