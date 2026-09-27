import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

class Input {
    void read() throws IOException {
        InputStream in = new FileInputStream("data.txt");
        in.read();
    }
}