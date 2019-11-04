package wjapkg;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class main {
    private static DrawSee graph;
    public static void main(String []args) throws IOException {
        graph = new DrawSee();
        int port = 54321;
        ServerSocket server = new ServerSocket(port);
        try {
            while(true) {
                Socket socket = server.accept();
                new ServerThread(socket);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            server.close();
        }
    }
    private static class ServerThread extends Thread {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private int last_x;
        private int last_y;

        public ServerThread(Socket s) throws IOException{
            this.socket = s;
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(),true);
            start();
        }

        public void run() {
            try {
                //-------------
                int ReceiveAccelerometer = 0;
                int ReceiveMagnetic = 0;
                int DrawStart = 0;
                float[] R = new float[9];
                float[] acceleration = new float[3];
                float[] magnet = new float[3];
                float[] values = new float[3];
                while (true) {
                    String str = in.readLine();
                    if (str == null || "".equals(str.trim())) continue;
                    String[] strs = str.split(",");
                    String type = strs[0];
                    float x = 0.0f;
                    float y = 0.0f;

                    float z = 0.0f;
                    if (type.equals("ACCELEROMETER")) {
                        x = Float.valueOf(strs[1]);
                        y = Float.valueOf(strs[2]);
                        z = Float.valueOf(strs[3]);
                        acceleration[0] = x;
                        acceleration[1] = y;
                        acceleration[2] = z;
                        ReceiveAccelerometer = 1;
                    } else if (type.equals("MAGNETIC")) {
                        x = Float.valueOf(strs[1]);
                        y = Float.valueOf(strs[2]);
                        z = Float.valueOf(strs[3]);
                        magnet[0] = x;
                        magnet[1] = y;
                        magnet[2] = z;
                        ReceiveMagnetic = 1;
                    } else if (type.equals("PRESSURE")) {
                        x = Float.valueOf(strs[1]);
                        System.out.println("Receive Msg " + type + ": " + " x:" + x + " y:" + y + " z:" + z);
                        continue;
                    } else if (type.equals("EXPRESSION")) {
                        System.out.println("Receive Msg " + type + ": " + " x:" + x + " y:" + y + " z:" + z);
                        Calculator cal = new Calculator();
                        float ans = (float) cal.calculate(strs[1]);
                        String sendText = Float.toString(ans);
                        out.println("EXPRESSION," + sendText);
                        out.flush();
                    } else if (type.equals("TOUCH")) {
                        String ttp = strs[1];
                        x = Float.valueOf(strs[2]);
                        y = Float.valueOf(strs[3]);
                        if (ttp.equals("DOWN")) {
                            last_x = (int) x;
                            last_y = (int) y;
                            DrawStart = 1;
                        }
                        if (ttp.equals("UP")) {
                            graph.paint(last_x, last_y, (int) x, (int) y);
                            DrawStart = 0;
                        }
                        System.out.println("Receive Msg " + type + " " + ttp + " x:" + x + " y:" + y);
                        if (DrawStart == 1) {
                            System.out.println("lastX:"+last_x+" lastY:"+last_y+" x:"+(int)x+" y:"+(int)y);
                            graph.paint(last_x, last_y, (int) x, (int) y);
                            last_x = (int) x;
                            last_y = (int) y;
                        }
                        continue;
                    } else continue;
                    System.out.println("Receive Msg " + type + ": " + " x:" + x + " y:" + y + " z:" + z);
                    if (ReceiveAccelerometer == 1 && ReceiveMagnetic == 1) {
                        ReceiveAccelerometer = 0;
                        ReceiveMagnetic = 0;
                        getRotationMatrix(R, acceleration, magnet);
                        getOrientation(R, values);
                        String sendtext = Float.toString(values[0]);
                        System.out.println("sendtext" + sendtext);
                        out.println("ROTATE," + sendtext);
                        out.flush();
                    }
                }
                //-------------
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public static boolean getRotationMatrix(float[] R, float[] gravity, float[] geomagnetic) {
        float Ax = gravity[0];
        float Ay = gravity[1];
        float Az = gravity[2];
        final float Ex = geomagnetic[0];
        final float Ey = geomagnetic[1];
        final float Ez = geomagnetic[2];
        float Hx = Ey*Az - Ez*Ay;
        float Hy = Ez*Ax - Ex*Az;
        float Hz = Ex*Ay - Ey*Ax;
        final float normH = (float)Math.sqrt(Hx*Hx + Hy*Hy + Hz*Hz);
        if (normH < 0.1f) {
            // device is close to free fall (or in space?), or close to
            // magnetic north pole. Typical values are  > 100.
            return false;
        }
        final float invH = 1.0f / normH;
        Hx *= invH;
        Hy *= invH;
        Hz *= invH;
        final float invA = 1.0f / (float)Math.sqrt(Ax*Ax + Ay*Ay + Az*Az);
        Ax *= invA;
        Ay *= invA;
        Az *= invA;
        final float Mx = Ay*Hz - Az*Hy;
        final float My = Az*Hx - Ax*Hz;
        final float Mz = Ax*Hy - Ay*Hx;
        if (R != null) {
            if (R.length == 9) {
                R[0] = Hx;     R[1] = Hy;     R[2] = Hz;
                R[3] = Mx;     R[4] = My;     R[5] = Mz;
                R[6] = Ax;     R[7] = Ay;     R[8] = Az;
            } else if (R.length == 16) {
                R[0]  = Hx;    R[1]  = Hy;    R[2]  = Hz;   R[3]  = 0;
                R[4]  = Mx;    R[5]  = My;    R[6]  = Mz;   R[7]  = 0;
                R[8]  = Ax;    R[9]  = Ay;    R[10] = Az;   R[11] = 0;
                R[12] = 0;     R[13] = 0;     R[14] = 0;    R[15] = 1;
            }
        }
        return true;
    }

    public static boolean getOrientation(float[] R, float values[]) {
        /*
         * 4x4 (length=16) case:
         *   /  R[ 0]   R[ 1]   R[ 2]   0  \
         *   |  R[ 4]   R[ 5]   R[ 6]   0  |
         *   |  R[ 8]   R[ 9]   R[10]   0  |
         *   \      0       0       0   1  /
         *
         * 3x3 (length=9) case:
         *   /  R[ 0]   R[ 1]   R[ 2]  \
         *   |  R[ 3]   R[ 4]   R[ 5]  |
         *   \  R[ 6]   R[ 7]   R[ 8]  /
         *
         */
        if (R.length == 9) {
            values[0] = (float)Math.atan2(R[1], R[4]);
            values[1] = (float)Math.asin(-R[7]);
            values[2] = (float)Math.atan2(-R[6], R[8]);
        } else {
            values[0] = (float)Math.atan2(R[1], R[5]);
            values[1] = (float)Math.asin(-R[9]);
            values[2] = (float)Math.atan2(-R[8], R[10]);
        }
        return true;
    }
}
