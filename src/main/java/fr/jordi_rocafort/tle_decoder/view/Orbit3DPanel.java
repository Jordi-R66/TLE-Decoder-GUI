package fr.jordi_rocafort.tle_decoder.view;

import fr.jordi_rocafort.tle_decoder.model.data.Coords3D;
import fr.jordi_rocafort.tle_decoder.model.physics.Constants;
import fr.jordi_rocafort.tle_decoder.util.ResourceManager;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

import org.jogamp.java3d.*;
import org.jogamp.vecmath.*;
import org.jogamp.java3d.utils.universe.*;
import org.jogamp.java3d.utils.geometry.*;
import org.jogamp.java3d.utils.image.TextureLoader;

public class Orbit3DPanel extends JPanel {
	private static Orbit3DPanel instance;
	private SimpleUniverse universe;

	private TransformGroup earthRotGroup;
	private TransformGroup satTransformGroup;
	private TransformGroup viewTransform;
	private Shape3D trackShape;

	private static final double SCALE = 1.0 / Constants.WGS84_A;

	public static Orbit3DPanel getInstance() {
		if (instance == null) {
			instance = new Orbit3DPanel();
		}
		return instance;
	}

	private Orbit3DPanel() {
		this.setLayout(new BorderLayout());
		this.setBorder(BorderFactory.createTitledBorder("Vue 3D Globale (ECI - Anneau fixe & Terre en rotation)"));

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

		BufferedImage sharedMapImage = ResourceManager.getLowResWorldMap();

		if (sharedMapImage != null) {
			TextureLoader loader = new TextureLoader(sharedMapImage, (String) null);
			earthAppearance.setTexture(loader.getTexture());

			Material material = new Material();
			material.setLightingEnable(true);
			earthAppearance.setMaterial(material);
		}

		int primFlags = Primitive.GENERATE_NORMALS | Primitive.GENERATE_TEXTURE_COORDS;
		Sphere earthSphere = new Sphere(1.0f, primFlags, 100, earthAppearance);

		double flattening = 1.0 / 298.257223563;
		double polarRatio = 1.0 - flattening;
		Transform3D earthScaleT3d = new Transform3D();
		earthScaleT3d.setScale(new Vector3d(1.0, polarRatio, 1.0));
		TransformGroup earthScaleGroup = new TransformGroup(earthScaleT3d);
		earthScaleGroup.addChild(earthSphere);

		earthRotGroup = new TransformGroup();
		earthRotGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		earthRotGroup.addChild(earthScaleGroup);
		root.addChild(earthRotGroup);

		// 2. --- LE SATELLITE ---
		satTransformGroup = new TransformGroup();
		satTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

		Appearance satApp = new Appearance();
		ColoringAttributes ca = new ColoringAttributes(new Color3f(1.0f, 0.0f, 0.0f), ColoringAttributes.FASTEST);
		satApp.setColoringAttributes(ca);
		Sphere satSphere = new Sphere(0.01f, satApp);

		satTransformGroup.addChild(satSphere);
		root.addChild(satTransformGroup);

		// 3. --- LE TRACÉ DE L'ORBITE ---
		trackShape = new Shape3D();
		trackShape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);

		Appearance trackApp = new Appearance();
		ColoringAttributes trackColor = new ColoringAttributes(new Color3f(1.0f, 1.0f, 1.0f),
				ColoringAttributes.FASTEST);
		trackApp.setColoringAttributes(trackColor);
		LineAttributes lineAttr = new LineAttributes();
		lineAttr.setLineWidth(2.0f);
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

	public void updatePosition(Coords3D eciCoords, long timestamp) {
		if (satTransformGroup == null || eciCoords == null)
			return;

		double x = eciCoords.x() * SCALE;
		double y = eciCoords.z() * SCALE;
		double z = -eciCoords.y() * SCALE;

		Vector3d satPosition = new Vector3d(x, y, z);
		Transform3D t3dSat = new Transform3D();
		t3dSat.setTranslation(satPosition);
		satTransformGroup.setTransform(t3dSat);

		// 1. Calcul de la distance du satellite depuis le centre (en Rayons Terrestres)
		double r3d = Math.sqrt(x * x + y * y + z * z);

		// 2. Écart absolu : On force la caméra à être TOUJOURS 1.5 rayons terrestres
		// plus loin
		double constantOffset = 1.5;
		double distMultiplier = (r3d + constantOffset) / r3d;

		Point3d camPos = new Point3d(x * distMultiplier, y * distMultiplier, z * distMultiplier);

		Vector3d upVector = new Vector3d(0, 1, 0);
		if (Math.abs(y / Math.sqrt(x * x + y * y + z * z)) > 0.98) {
			upVector = new Vector3d(1, 0, 0);
		}

		Transform3D cameraT3D = new Transform3D();
		cameraT3D.lookAt(camPos, new Point3d(0, 0, 0), upVector);
		cameraT3D.invert();
		viewTransform.setTransform(cameraT3D);

		double jd = (timestamp / 86400.0) + 2440587.5;
		double t = (jd - 2451545.0) / 36525.0;
		double gmstDeg = 280.46061837 + 360.98564736629 * (jd - 2451545.0) + 0.000387933 * t * t;
		double gmstRad = Math.toRadians(gmstDeg % 360.0);

		// LA CORRECTION FINALE : + 90 degrés (Math.PI / 2.0)
		Transform3D rotY = new Transform3D();
		rotY.rotY(gmstRad + Math.PI / 2.0);
		if (earthRotGroup != null)
			earthRotGroup.setTransform(rotY);
	}

	public void setFutureTrack(List<Coords3D> track3D) {
		if (track3D == null || track3D.isEmpty() || trackShape == null)
			return;

		int numPoints = track3D.size();
		Point3f[] points = new Point3f[numPoints];

		for (int i = 0; i < numPoints; i++) {
			Coords3D c = track3D.get(i);
			double x = c.x() * SCALE;
			double y = c.z() * SCALE;
			double z = -c.y() * SCALE;
			points[i] = new Point3f((float) x, (float) y, (float) z);
		}

		LineStripArray lineArray = new LineStripArray(numPoints, GeometryArray.COORDINATES, new int[] { numPoints });
		lineArray.setCoordinates(0, points);
		trackShape.setGeometry(lineArray);
	}
}