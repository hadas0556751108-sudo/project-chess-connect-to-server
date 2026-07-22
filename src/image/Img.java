package image;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Lightweight image‑utility class using only standard JDK APIs.
 */
public class Img {

    private BufferedImage img;

    /* ----------- load & optional resize ----------- */
    public Img read(String path,
                    Dimension targetSize,
                    boolean keepAspect,
                    Object interpolation /*ignored*/) {

        try {
            img = ImageIO.read(new File(path));                              // :contentReference[oaicite:0]{index=0}
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot load image: " + path);
        }
        if (img == null) throw new IllegalArgumentException("Unsupported image: " + path);

        if (targetSize != null) {
            int tw = targetSize.width, th = targetSize.height;
            int w = img.getWidth(), h = img.getHeight();

            int nw, nh;
            if (keepAspect) {                                                // :contentReference[oaicite:1]{index=1}
                double s = Math.min(tw / (double) w, th / (double) h);
                nw = (int) Math.round(w * s);
                nh = (int) Math.round(h * s);
            } else { nw = tw; nh = th; }

            BufferedImage dst = new BufferedImage(
                    nw, nh,
                    img.getColorModel().hasAlpha()
                            ? BufferedImage.TYPE_INT_ARGB
                            : BufferedImage.TYPE_INT_RGB);

            Graphics2D g = dst.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                               RenderingHints.VALUE_INTERPOLATION_BILINEAR);   // :contentReference[oaicite:2]{index=2}
            g.drawImage(img, 0, 0, nw, nh, null);
            g.dispose();
            img = dst;
        }
        return this;
    }

    public Img read(String path) { return read(path, null, false, null); }

    /* ----------- draw this image onto another ----------- */
    public void drawOn(Img other, int x, int y) {
        if (img == null || other.img == null)
            throw new IllegalStateException("Both images must be loaded.");

        if (x + img.getWidth()  > other.img.getWidth()
         || y + img.getHeight() > other.img.getHeight())
            throw new IllegalArgumentException("Patch exceeds destination bounds.");

        Graphics2D g = other.img.createGraphics();
        g.setComposite(AlphaComposite.SrcOver);                               // handles alpha channel :contentReference[oaicite:3]{index=3}
        g.drawImage(img, x, y, null);                                        // :contentReference[oaicite:4]{index=4}
        g.dispose();
    }

    /* ----------- annotate with text ----------- */
    public void putText(String txt, int x, int y, float fontSize,
                        Color color, int thickness /*unused in Java2D*/) {

        if (img == null) throw new IllegalStateException("Image not loaded.");

        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                           RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(color);
        g.setFont(img.getGraphics().getFont().deriveFont(fontSize * 12));     // simple scale
        g.drawString(txt, x, y);                                             // :contentReference[oaicite:5]{index=5}
        g.dispose();
    }

    /* ----------- display in a Swing window ----------- */
    public void show() {
        if (img == null) throw new IllegalStateException("Image not loaded.");

        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Image");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.add(new JLabel(new ImageIcon(img)));                            // :contentReference[oaicite:6]{index=6}
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }

    /* ----------- draw rectangle ----------- */
    public void drawRect(int x, int y, int width, int height, Color color, int thickness) {
        if (img == null) {
            throw new IllegalStateException("Image not loaded.");
        }

        Graphics2D graphics = img.createGraphics();
        graphics.setColor(color);
        graphics.setStroke(new BasicStroke(thickness));
        graphics.drawRect(x, y, width, height);
        graphics.dispose();
    }

    /* ----------- copy image ----------- */
    public Img copy() {
        if (img == null) {
            throw new IllegalStateException("Image not loaded.");
        }

        BufferedImage copy = new BufferedImage(
                img.getWidth(),
                img.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D graphics = copy.createGraphics();
        graphics.drawImage(img, 0, 0, null);
        graphics.dispose();

        Img result = new Img();
        result.img = copy;

        return result;
    }

    /* ----------- access (optional) ----------- */
    public BufferedImage get() { return img; }
}
