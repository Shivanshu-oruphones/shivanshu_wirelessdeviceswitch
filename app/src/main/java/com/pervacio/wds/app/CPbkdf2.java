package com.pervacio.wds.app;

/*
 * Copyright (c) 2012
 * Cole Barnes [cryptofreek{at}gmail{dot}com]
 * http://cryptofreek.org/
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * -----------------------------------------------------------------------------
 *
 */

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Formatter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class CPbkdf2
{
    /* START RFC 2898 IMPLEMENTATION */
    public static byte[] derive(byte[] aPassword, byte[] aSalt, int aIterations, int aKeyLength)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try
        {
            int hLen = 20;

            if (aKeyLength > ((Math.pow(2, 32)) - 1) * hLen)
            {
                System.out.println("derived key too long");
            }
            else
            {
                int l = (int) Math.ceil((double) aKeyLength / (double) hLen);
                // int r = aKeyLength - (l-1)*hLen;

                for (int i = 1; i <= l; i++)
                {
                    byte[] T = F(aPassword, aSalt, aIterations, i);
                    baos.write(T);
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        byte[] baDerived = new byte[aKeyLength];
        System.arraycopy(baos.toByteArray(), 0, baDerived, 0, baDerived.length);

        return baDerived;
    }

    private static byte[] F(byte[] aPassword, byte[] aSalt, int aIterations, int i) throws Exception
    {
        byte[] U_LAST = null;
        byte[] U_XOR = null;

        SecretKeySpec key = new SecretKeySpec(aPassword, "HmacSHA1");
        Mac mac = Mac.getInstance(key.getAlgorithm());
        mac.init(key);

        for (int j = 0; j < aIterations; j++)
        {
            if (j == 0)
            {
                byte[] baS = aSalt;
                byte[] baI = INT(i);
                byte[] baU = new byte[baS.length + baI.length];

                System.arraycopy(baS, 0, baU, 0, baS.length);
                System.arraycopy(baI, 0, baU, baS.length, baI.length);

                U_XOR = mac.doFinal(baU);
                U_LAST = U_XOR;
                mac.reset();
            }
            else
            {
                byte[] baU = mac.doFinal(U_LAST);
                mac.reset();

                for (int k = 0; k < U_XOR.length; k++)
                {
                    U_XOR[k] = (byte) (U_XOR[k] ^ baU[k]);
                }

                U_LAST = baU;
            }
        }

        return U_XOR;
    }

    private static byte[] INT(int i)
    {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putInt(i);

        return bb.array();
    }
  /* END RFC 2898 IMPLEMENTATION */

    /* START HELPER FUNCTIONS */
    private static String toHex(byte[] ba)
    {
        String strHex = null;

        if (ba != null)
        {
            StringBuilder sb = new StringBuilder(ba.length * 2);
            Formatter formatter = new Formatter(sb);

            for (byte b : ba)
            {
                formatter.format("%02x", b);
            }

            formatter.close();
            strHex = sb.toString().toLowerCase();
        }

        return strHex;
    }
}