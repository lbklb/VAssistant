package cn.vove7.androlua.luabridge;

import com.luajava.LuaException;
import com.luajava.LuaState;

import cn.vove7.androlua.LuaApp;
import cn.vove7.androlua.LuaHelper;
import cn.vove7.androlua.luautils.LuaManagerI;

import static cn.vove7.androlua.luabridge.LuaUtil.errorReason;

/**
 * @author SYSTEM
 * <p>封装
 * 2018/8/3
 */
public class LuaFunHelper {

    private LuaHelper luaHelper;
    private LuaState L;

    public LuaFunHelper(LuaHelper luaHelper, LuaState l) {
        this.luaHelper = luaHelper;
        L = l;
    }

    public void newLuaThread(String str, Object... args) throws LuaException {
        luaHelper.autoRun(str, args);
    }

    public void newLuaThread(byte[] buf, Object... args) throws LuaException {
        L.setTop(0);
        int ok = L.LloadBuffer(buf, "Thread");
        if (ok == 0) {
            L.getGlobal("debug");
            L.getField(-1, "traceback");
            L.remove(-2);
            L.insert(-2);
            int l = args.length;
            for (Object arg : args) {
                L.pushObjectValue(arg);
            }
            ok = L.pcall(l, 0, -2 - l);
            if (ok == 0) {
                return;
            }
        }
        throw new LuaException(errorReason(ok) + ": " + L.toString(-1));
    }


    public void runFunc(String funcName, Object... args) throws LuaException {
        L.setTop(0);
        L.getGlobal(funcName);
        if (L.isFunction(-1)) {
            L.getGlobal("debug");
            L.getField(-1, "traceback");
            L.remove(-2);
            L.insert(-2);

            int l = args.length;
            for (Object arg : args) {
                L.pushObjectValue(arg);
            }

            int ok = L.pcall(l, 1, -2 - l);
            if (ok == 0) {
                return;
            }
            throw new LuaException(errorReason(ok) + ": " + L.toString(-1));
        }
    }

    public void setField(String key, Object value) throws LuaException {
        L.pushObjectValue(value);
        L.setGlobal(key);
    }
}
