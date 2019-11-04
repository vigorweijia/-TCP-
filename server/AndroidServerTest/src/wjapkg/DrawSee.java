package wjapkg;

import javax.swing.*;
import java.awt.*;

public class DrawSee extends JFrame {
    private Color rectcolor = new Color(0xf5f5f5);
    private Graphics2D G;
    public DrawSee() {
        Container p = getContentPane();
        setBounds(0,0,1400,700);
        setVisible(true);
        p.setBackground(rectcolor);
        setLayout(null);
        setResizable(false);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        try {
            Thread.sleep(500);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        Graphics g = this.getGraphics();
        G = (Graphics2D)g;
    }
    public void paint(int x1, int y1, int x2, int y2) {
        Color linecolor = new Color(0xff6600);
        G.setColor(linecolor);
        G.setStroke(new BasicStroke(3.0f));
        G.drawLine(x1, y1, x2, y2);
    }
}
