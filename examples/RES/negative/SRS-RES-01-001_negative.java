import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

class SRS_RES_01_001_negative {
    void read() throws IOException {
        InputStream in = new FileInputStream("data.txt");
        in.read();
    }
}