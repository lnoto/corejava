package query.page.read;

import java.time.LocalDateTime;

public interface ReadPage {
    short version();

    int pageNumber();

    int totalRecords();

    LocalDateTime createdTime();

    int next(byte[] buffer);

    boolean hasNext();

    static ReadPage create(byte[] buffer) {
        return new ReadableSlottedPage(buffer);
    }
}
