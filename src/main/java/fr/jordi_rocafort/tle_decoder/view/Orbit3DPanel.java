package fr.jordi_rocafort.tle_decoder.view;

import fr.jordi_rocafort.tle_decoder.model.data.GeoCoords;
import fr.jordi_rocafort.tle_decoder.model.physics.Constants;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.List;

import org.jogamp.java3d.*;
import org.jogamp.vecmath.*;
import org.jogamp.java3d.utils.universe.*;
import org.jogamp.java3d.utils.geometry.*;
import org.jogamp.java3d.utils.image.TextureLoader;

public class Orbit3DPanel extends JPanel {
	private static Orbit3DPanel instance;
	private SimpleUniverse universe;
	private TransformGroup satTransformGroup;
	private TransformGroup viewTransform; // Pour contrôler la caméra
	private Shape3D trackShape; // Pour le tracé de l'orbite

	private static final double SCALE = 1.0 / Constants.WGS84_A;

	public static Orbit3DPanel getInstance() {
		if (instance == null) {
			instance = new Orbit3DPanel();
		}
		return instance;
	}

	private Orbit3DPanel() {
		this.setLayout(new BorderLayout());
		this.setBorder(BorderFactory.createTitledBorder("Vue 3D Globale (Tracking Auto)"));

		GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
		Canvas3D canvas = new Canvas3D(config);
		this.add(canvas, BorderLayout.CENTER);

		universe = new SimpleUniverse(canvas);
		BranchGroup scene = createSceneGraph();

		universe.getViewingPlatform().setNominalViewingTransform();
		viewTransform = universe.getViewingPlatform().getViewPlatformTransform();

		universe.addBranchGraph(scene);
	}

	private BranchGroup createSceneGraph() {
		BranchGroup root = new BranchGroup();

		// 1. --- LA TERRE ---
		Appearance earthAppearance = new Appearance();
		URL textureUrl = getClass().getResource("/world_map.jpg");
		if (textureUrl != null) {
			TextureLoader loader = new TextureLoader(textureUrl, null);
			earthAppearance.setTexture(loader.getTexture());
			Material material = new Material();
			material.setLightingEnable(true);
			earthAppearance.setMaterial(material);
		}

		int primFlags = Primitive.GENERATE_NORMALS | Primitive.GENERATE_TEXTURE_COORDS;
		Sphere earthSphere = new Sphere(1.0f, primFlags, 100, earthAppearance);

		double flattening = 1.0 / 298.257223563;
		double polarRatio = 1.0 - flattening;

		Transform3D earthTransform = new Transform3D();
		earthTransform.setScale(new Vector3d(1.0, polarRatio, 1.0));

		TransformGroup earthTg = new TransformGroup(earthTransform);
		earthTg.addChild(earthSphere);
		root.addChild(earthTg);

		// 2. --- LE SATELLITE ---
		satTransformGroup = new TransformGroup();
		satTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

		Appearance satApp = new Appearance();
		ColoringAttributes ca = new ColoringAttributes(new Color3f(1.0f, 0.0f, 0.0f), ColoringAttributes.FASTEST);
		satApp.setColoringAttributes(ca);
		Sphere satSphere = new Sphere(0.02f, satApp);

		satTransformGroup.addChild(satSphere);
		root.addChild(satTransformGroup);

		// 3. --- LE TRACÉ DE L'ORBITE 3D ---
		trackShape = new Shape3D();
		trackShape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE); // Permet de modifier la ligne en temps réel

		Appearance trackApp = new Appearance();
		ColoringAttributes trackColor = new ColoringAttributes(new Color3f(1.0f, 1.0f, 1.0f),
				ColoringAttributes.FASTEST);
		trackApp.setColoringAttributes(trackColor);
		LineAttributes lineAttr = new LineAttributes();
		lineAttr.setLineWidth(2.0f); // Épaisseur de la ligne
		lineAttr.setLineAntialiasingEnable(true);
		trackApp.setLineAttributes(lineAttr);

		trackShape.setAppearance(trackApp);
		root.addChild(trackShape);

		// 4. --- LUMIÈRES ---
		BoundingSphere bounds = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 1000.0);
		DirectionalLight sunLight = new DirectionalLight(new Color3f(1.0f, 1.0f, 0.9f),
				new Vector3f(1.0f, 0.0f, -1.0f));
		sunLight.setInfluencingBounds(bounds);
		root.addChild(sunLight);

		AmbientLight ambient = new AmbientLight(new Color3f(0.3f, 0.3f, 0.4f));
		ambient.setInfluencingBounds(bounds);
		root.addChild(ambient);

		return root;
	}

	public void updatePosition(GeoCoords geoCoords) {
		if (satTransformGroup == null || geoCoords == null)
			return;

		double latRad = Math.toRadians(geoCoords.lat());
		double lonRad = Math.toRadians(geoCoords.lng());
		double r = (Constants.WGS84_A + geoCoords.altitude()) * SCALE;

		double x = r * Math.cos(latRad) * Math.sin(lonRad);
		double y = r * Math.sin(latRad);
		double z = r * Math.cos(latRad) * Math.cos(lonRad);

		// Mise à jour de la sphère rouge
		Vector3d satPosition = new Vector3d(x, y, z);
		Transform3D t3dSat = new Transform3D();
		t3dSat.setTranslation(satPosition);
		satTransformGroup.setTransform(t3dSat);

		// --- CAMERA CINÉMATIQUE ---
		// La caméra est placée derrière le satellite (1.5 fois la distance de l'orbite)
		Point3d camPos = new Point3d(x * 1.5, y * 1.5, z * 1.5);

		// Protection contre le "Gimbal Lock" si le satellite passe exactement au-dessus
		// des pôles (Axe Y)
		Vector3d upVector = new Vector3d(0, 1, 0);
		if (Math.abs(y / r) > 0.98) {
			upVector = new Vector3d(1, 0, 0);
		}

		Transform3D cameraT3D = new Transform3D();
		cameraT3D.lookAt(camPos, new Point3d(0, 0, 0), upVector); // Regarde vers la Terre (0,0,0)
		cameraT3D.invert(); // Java3D exige d'inverser la matrice pour le ViewPlatform
		viewTransform.setTransform(cameraT3D);
	}

	/**
	 * Dessine la ligne de l'orbite complète autour de la Terre
	 */
	public void setFutureTrack(List<GeoCoords> futureTrack) {
		if (futureTrack == null || futureTrack.isEmpty() || trackShape == null)
			return;

		int numPoints = futureTrack.size();
		Point3f[] points = new Point3f[numPoints];

		for (int i = 0; i < numPoints; i++) {
			GeoCoords geo = futureTrack.get(i);
			double latRad = Math.toRadians(geo.lat());
			double lonRad = Math.toRadians(geo.lng());
			double r = (Constants.WGS84_A + geo.altitude()) * SCALE;

			double x = r * Math.cos(latRad) * Math.sin(lonRad);
			double y = r * Math.sin(latRad);
			double z = r * Math.cos(latRad) * Math.cos(lonRad);

			points[i] = new Point3f((float) x, (float) y, (float) z);
		}

		// Création de la ligne continue
		LineStripArray lineArray = new LineStripArray(numPoints, GeometryArray.COORDINATES, new int[] { numPoints });
		lineArray.setCoordinates(0, points);

		// Application au composant 3D
		trackShape.setGeometry(lineArray);
	}
}