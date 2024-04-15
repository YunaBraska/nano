package berlin.yuna.nano.services.http.model;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

public class HttpResponse {
    private final int statusCode;
    private final byte[] body;
    private final Map<String, String> headers;

    public HttpResponse(final int statusCode,final byte[] body,final Map<String, String> headers) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public byte[] getBody() {
        return body;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        HttpResponse that = (HttpResponse) object;
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
