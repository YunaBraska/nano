package berlin.yuna.nano.services.http.model;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

public class HttpResponse {

    private int statusCode;
    private byte[] body;
    private Map<String, String> headers;
    // merge with HttpRequest as HttpObject
    //add content type

    public HttpResponse statusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public HttpResponse body(byte[] body) {
        this.body = body;
        return this;
    }

    public HttpResponse headers(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    public int statusCode() {
        return statusCode;
    }

    public byte[] body() {
        return body;
    }

    public Map<String, String> headers() {
        return headers;
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        final HttpResponse that = (HttpResponse) object;
        return statusCode == that.statusCode && Arrays.equals(body, that.body) && Objects.equals(headers, that.headers);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(statusCode, headers);
        result = 31 * result + Arrays.hashCode(body);
        return result;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", HttpResponse.class.getSimpleName() + "[", "]")
            .add("statusCode=" + statusCode)
            .add("body=" + Arrays.toString(body))
            .add("headers=" + headers)
            .toString();
    }
}
