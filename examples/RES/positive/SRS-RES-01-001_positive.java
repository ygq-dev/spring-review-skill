import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

class SRS_RES_01_001_positive {
    void read() throws IOException {
        try (InputStream in = new FileInputStream("data.txt")) {
            in.read();
        }
    }
}