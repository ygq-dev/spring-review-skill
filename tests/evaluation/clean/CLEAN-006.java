package evaluation.clean;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class CLEAN_006 {
    public void read() throws IOException {
        try (InputStream in = new FileInputStream("data.txt")) {
            in.read();
        }
    }
}
