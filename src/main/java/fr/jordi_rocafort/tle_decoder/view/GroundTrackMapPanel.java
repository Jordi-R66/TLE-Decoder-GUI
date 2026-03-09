package fr.jordi_rocafort.tle_decoder.view;

import fr.jordi_rocafort.tle_decoder.model.data.GeoCoords;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;

public class GroundTrackMapPanel extends JPanel {

    private static GroundTrackMapPanel instance;
    private GeoCoords currentPosition;

    // Historique et futur
    private final LinkedList<GeoCoords> trackHistory = new LinkedList<>();
    private List<GeoCoords> futureTrack = new ArrayList<>();
    private static final int MAX_HISTORY = 300;

    public static GroundTrackMapPanel getInstance() {
        if (instance == null) {
            instance = new GroundTrackMapPanel();
        }
        return instance;
    }

    private GroundTrackMapPanel() {
        this.setBackground(Color.BLACK); // Fond global noir pour les bandes
        this.setBorder(BorderFactory.createTitledBorder("Ground Track (Projection Équirectangulaire)"));
    }

    public void updatePosition(GeoCoords geoCoords) {
        this.currentPosition = geoCoords;
        trackHistory.add(geoCoords);
        if (trackHistory.size() > MAX_HISTORY) {
            trackHistory.removeFirst();
        }
        this.repaint();
    }

    public void setFutureTrack(List<GeoCoords> futureTrack) {
        this.futureTrack = futureTrack;
        this.repaint();
    }

    public void clearTrack() {
        trackHistory.clear();
        futureTrack.clear();
        currentPosition = null;
        this.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int panelWidth = getWidth();
        int panelHeight = getHeight();

        // 1. Calcul de la zone de dessin (Ratio strict 2:1)
        int drawWidth, drawHeight, offsetX, offsetY;

        if (panelWidth > panelHeight * 2) {
            // Le panel est trop large -> Bandes noires sur les côtés
            drawHeight = panelHeight;
            drawWidth = panelHeight * 2;
            offsetX = (panelWidth - drawWidth) / 2;
            offsetY = 0;
        } else {
            // Le panel est trop haut -> Bandes noires en haut et en bas
            drawWidth = panelWidth;
            drawHeight = panelWidth / 2;
            offsetX = 0;
            offsetY = (panelHeight - drawHeight) / 2;
        }

        // 2. Dessin du fond de la carte
        g2d.setColor(new Color(20, 24, 32));
        g2d.fillRect(offsetX, offsetY, drawWidth, drawHeight);

        // 3. Dessin de la grille
        g2d.setColor(new Color(40, 50, 65));
        for (int lon = -180; lon <= 180; lon += 30) {
            int x = lngToX(lon, drawWidth, offsetX);
            g2d.drawLine(x, offsetY, x, offsetY + drawHeight);
        }
        for (int lat = -90; lat <= 90; lat += 30) {
            int y = latToY(lat, drawHeight, offsetY);
            g2d.drawLine(offsetX, y, offsetX + drawWidth, y);
        }

        g2d.setColor(new Color(80, 90, 110));
        g2d.drawLine(offsetX, offsetY + drawHeight / 2, offsetX + drawWidth, offsetY + drawHeight / 2); // Équateur
        g2d.drawLine(offsetX + drawWidth / 2, offsetY, offsetX + drawWidth / 2, offsetY + drawHeight); // Greenwich

        // 4. Dessin de la trace FUTURE (En pointillés)
        if (futureTrack != null && !futureTrack.isEmpty()) {
            g2d.setColor(new Color(255, 255, 255, 120));
            g2d.setStroke(
                    new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[] { 5 }, 0));
            drawTrack(g2d, futureTrack, drawWidth, drawHeight, offsetX, offsetY);
        }

        // 5. Dessin de la trace HISTORIQUE (Ligne continue)
        if (trackHistory.size() > 1) {
            g2d.setColor(new Color(100, 200, 255, 180));
            g2d.setStroke(new BasicStroke(2.0f));
            drawTrack(g2d, trackHistory, drawWidth, drawHeight, offsetX, offsetY);
        }

        // 6. Dessin du satellite (point actuel)
        if (currentPosition != null) {
            int currentX = lngToX(currentPosition.lng(), drawWidth, offsetX);
            int currentY = latToY(currentPosition.lat(), drawHeight, offsetY);

            g2d.setColor(new Color(255, 50, 50, 100));
            g2d.fillOval(currentX - 10, currentY - 10, 20, 20);

            g2d.setColor(Color.RED);
            g2d.fillOval(currentX - 4, currentY - 4, 8, 8);
        }
    }

    private void drawTrack(Graphics2D g2d, Iterable<GeoCoords> track, int drawWidth, int drawHeight, int offsetX,
            int offsetY) {
        Path2D path = new Path2D.Double();
        boolean first = true;
        GeoCoords prev = null;

        for (GeoCoords pos : track) {
            int x = lngToX(pos.lng(), drawWidth, offsetX);
            int y = latToY(pos.lat(), drawHeight, offsetY);

            if (first) {
                path.moveTo(x, y);
                first = false;
            } else {
                if (Math.abs(pos.lng() - prev.lng()) > 180) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }
            prev = pos;
        }
        g2d.draw(path);
    }

    private int lngToX(double lng, int drawWidth, int offsetX) {
        return offsetX + (int) ((lng + 180.0) / 360.0 * drawWidth);
    }

    private int latToY(double lat, int drawHeight, int offsetY) {
        return offsetY + (int) ((90.0 - lat) / 180.0 * drawHeight);
    }
}