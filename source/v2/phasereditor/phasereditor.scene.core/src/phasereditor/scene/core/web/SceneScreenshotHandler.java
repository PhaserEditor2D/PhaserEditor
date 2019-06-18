package phasereditor.scene.core.web;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.json.JSONObject;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.scene.core.PackReferencesCollector;
import phasereditor.scene.core.SceneCore;
import phasereditor.scene.core.SceneModel;
import phasereditor.webrun.core.WebRunCore;

public class SceneScreenshotHandler extends ServletContextHandler {

	public SceneScreenshotHandler() {
		setContextPath("/sceneScreenshotService");
		addServlet(new ServletHolder(new SceneScreenshotServlet()), "/sceneInfo/*");
	}

	class SceneScreenshotServlet extends HttpServlet {
		private static final long serialVersionUID = 1L;

		@Override
		protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			var reqData = IOUtils.toString(req.getInputStream(), "utf-8");
			var data = new JSONObject(reqData);

			java.nio.file.Path writeTo;
			{
				var filename = data.getString("file");
				var file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(filename));

				writeTo = SceneCore.getSceneScreenshotFile(file);
				SceneCore.saveScreenshotPath(file, writeTo);
			}

			{
				var imageData = data.getString("imageData");
				var i = imageData.indexOf(",");
				imageData = imageData.substring(i + 1);

				var bytes = Base64.getDecoder().decode(imageData.getBytes());

				try (var os = Files.newOutputStream(writeTo)) {
					IOUtils.write(bytes, os);
				}
			}
		}

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			resp.setContentType("application/json");

			var pathInfo = req.getPathInfo();

			var file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(pathInfo));

			if (file.exists()) {
				var data = new JSONObject();

				var sceneModel = new SceneModel();

				try {
					sceneModel.read(file);

					{
						var projectUrl = WebRunCore.getProjectBrowserURL(file.getProject(), false);
						data.put("projectUrl", projectUrl);
					}

					{
						var collector = new PackReferencesCollector(sceneModel,
								AssetPackCore.getAssetFinder(file.getProject()));

						var pack = collector.collectNewPack(m -> true);

						data.put("pack", pack);
					}

					{
						data.put("sceneModel", sceneModel.toJSON());
					}

				} catch (Exception e) {
					SceneCore.logError(e);
				}

				resp.getWriter().write(data.toString());
			}
		}
	}
}
