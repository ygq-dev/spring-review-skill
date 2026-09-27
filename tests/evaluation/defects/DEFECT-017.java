package evaluation.defects;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class DEFECT_017 {
    public void read() throws IOException {
        InputStream in = new FileInputStream("data.txt");
        in.read();
    }
}
