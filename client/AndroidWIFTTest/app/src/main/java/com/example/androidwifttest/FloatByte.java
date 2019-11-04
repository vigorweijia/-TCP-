package com.example.androidwifttest;

public class FloatByte {
    public static byte[] float2byte(float f) {
        int fbit = Float.floatToIntBits(f);
        byte[] b = new byte[4];
        for(int i = 0; i < 4; i++) b[3-i] = (byte)((fbit>>(24-i*8))&0xff);
        return b;
    }
    public static float byte2float(byte[] b) {
        int l;
        l = b[0];
        l &= 0xff;
        l |= ((int) b[1] << 8);
        l &= 0xffff;
        l |= ((int) b[2] << 16);
        l &= 0xffffff;
        l |= ((int) b[3] << 24);
        return Float.intBitsToFloat(l);
    }
}
