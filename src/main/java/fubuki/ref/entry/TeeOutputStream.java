package fubuki.ref.entry;

import java.io.IOException;
import java.io.OutputStream;

public class TeeOutputStream extends OutputStream {
    private final OutputStream outputStream1;
    private final OutputStream outputStream2;

    public TeeOutputStream(OutputStream outputStream1, OutputStream outputStream2) {
        this.outputStream1 = outputStream1;
        this.outputStream2 = outputStream2;
    }

    @Override
    public void write(int b) throws IOException {
        outputStream1.write(b);
        outputStream2.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        outputStream1.write(b);
        outputStream2.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        outputStream1.write(b, off, len);
        outputStream2.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        outputStream1.flush();
        outputStream2.flush();
    }

    @Override
    public void close() throws IOException {
        outputStream1.close();
        outputStream2.close();
    }
}
