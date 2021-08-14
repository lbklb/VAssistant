package cn.vove7.jarvis.jadb;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by xudong on 2/21/14.
 */
public interface AdbChannel extends Closeable {
    void readx(byte[] buffer, int length) throws IOException;

    void writex(AdbMessage message) throws IOException;
}
