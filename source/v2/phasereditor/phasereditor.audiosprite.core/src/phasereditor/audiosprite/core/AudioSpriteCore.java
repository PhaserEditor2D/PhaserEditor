// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions: The above copyright notice and this permission
// notice shall be included in all copies or substantial portions of the
// Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.
package phasereditor.audiosprite.core;

import static java.lang.System.out;
import static phasereditor.ui.PhaserEditorUI.eclipseFileToJavaPath;
import static phasereditor.ui.PhaserEditorUI.getNameFromFilename;
import static phasereditor.ui.PhaserEditorUI.logError;
import static phasereditor.ui.PhaserEditorUI.pickFileWithoutExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import phasereditor.audio.core.AudioCore;
import phasereditor.ui.PhaserEditorUI;

public class AudioSpriteCore {

	public static final String PLUGIN_ID = Activator.PLUGIN_ID;

	/**
	 * Check if the given content has an AudioSprite JSON format.
	 * 
	 * @param contents
	 *            The content to test.
	 * @param deepTest
	 *            If true, it also check the 'spritemap' has a valid syntax.
	 * @return Return <code>null</code> if the content has a valid format, else it
	 *         return an error message.
	 */
	public static String isAudioSpriteJSONContent(InputStream contents, boolean deepTest) {
		try {
			JSONTokener tokener = new JSONTokener(new InputStreamReader(contents));
			JSONObject obj = new JSONObject(tokener);
			JSONObject spritemap = obj.getJSONObject("spritemap");
			if (deepTest) {
				Iterator<String> keys = spritemap.keys();
				while (keys.hasNext()) {
					String key = keys.next();
					JSONObject sprite = spritemap.getJSONObject(key);
					sprite.getDouble("start");
					sprite.getDouble("end");
				}
			}
			return null;
		} catch (JSONException e) {
			return e.getMessage();
		}
	}

	/**
	 * Test if the content type of the file is the audio sprite type.
	 * 
	 * @param file
	 *            The file to test.
	 * @return If the file is an audio sprite.
	 * @throws CoreException
	 *             If error.
	 */
	public static boolean isAudioSpriteFile(IFile file) {
		if (!file.exists()) {
			return false;
		}

		try {
			var desc = file.getContentDescription();

			if (desc == null) {
				return false;
			}

			var contentType = desc.getContentType();

			if (contentType == null) {
				return false;
			}

			var id = contentType.getId();

			return id.equals(AudioSpritesDescriber.CONTENT_TYPE_ID);

		} catch (CoreException e) {
			logError(e);
		}

		return false;
	}

	public static IFile makeAudioSprite(List<IFile> audioFiles, IContainer dstDir, String audioSpritesName,
			Consumer<String> logger, IProgressMonitor monitor) throws Exception {
		IFile result = concatAudioSprite(new AudioSpritesModel(), audioFiles, dstDir, audioSpritesName, logger,
				monitor);
		return result;
	}

	public static IFile updateAudioSprite(AudioSpritesModel model, List<IFile> newAudioFiles, Consumer<String> logger,
			IProgressMonitor monitor) throws Exception {
		IContainer dstDir = model.getModelFile().getParent();
		String name = getNameFromFilename(model.getModelFile().getName());

		IFile spritesFile = pickFileWithoutExtension(model.getResources(), "mp3", "ogg");
		if (spritesFile != null && spritesFile.exists()) {
			newAudioFiles.add(0, spritesFile);
		}

		return concatAudioSprite(model, newAudioFiles, dstDir, name, logger, monitor);
	}

	private static IFile concatAudioSprite(AudioSpritesModel model, List<IFile> newAudioFiles, IContainer dstDir,
			String audioSpritesName, Consumer<String> logger, IProgressMonitor monitor) throws Exception {

		IFile jsonAudioSpritesFile = null;

		List<java.nio.file.Path> cancelFiles = new ArrayList<>();
		List<java.nio.file.Path> temporalFiles = new ArrayList<>();

		try {
			// ignore not audio files
			List<IFile> concatFiles = new ArrayList<>();
			for (IFile file : newAudioFiles) {
				if (AudioCore.isSupportedAudio(file)) {
					concatFiles.add(file);
				}
			}

			java.nio.file.Path silence = AudioCore.getSilenceAudioFile();

			// get the right audio file name

			String audioSpritesFileName = audioSpritesName + ".json";

			// prepare monitor task
			{
				// +2 convert to mp3 and ogg
				// +6 update cache for resulting files
				int totalWork = concatFiles.size() + 2 + 6;
				monitor.beginTask("Creating audio sprites '" + audioSpritesFileName + "'", totalWork);
			}

			IFile dstAudioFile = dstDir.getFile(new Path(audioSpritesName + ".wav"));

			{
				int i = 0;
				java.nio.file.Path join = null;
				boolean isUpdate = !model.getSprites().isEmpty();
				for (IFile file : concatFiles) {

					if (monitor.isCanceled()) {
						if (join != null) {
							cancelFiles.add(join);
						}
						throw new CancellationException();
					}

					java.nio.file.Path current = eclipseFileToJavaPath(file);

					monitor.subTask("Processing " + file.getName() + "...");

					String spritename = PhaserEditorUI.getNameFromFilename(file.getName());
					AudioSprite sprite = new AudioSprite();
					sprite.setName(spritename);

					if (!isUpdate || i > 0) {
						model.addSprite(sprite);
					}

					if (join == null) {
						// the first file

						join = current;

						double start = 0;
						double end = AudioCore.getSoundDuration(join);

						logger.accept(file.getFullPath().toPortableString() + " [" + start + ", " + end + "]");

						sprite.setStart(start);
						sprite.setEnd(end);

					} else {
						// concatenate the silence file

						List<java.nio.file.Path> concat = Arrays.asList(join, silence);

						java.nio.file.Path newJoin = Files.createTempFile("join-" + i + "-with-silence", ".wav");
						temporalFiles.add(newJoin);

						AudioCore.concatAudioFiles(concat, newJoin, logger);

						// concatenate the sound file

						join = newJoin;

						concat = Arrays.asList(join, current);

						newJoin = Files.createTempFile("join-" + i + "-", ".wav");
						temporalFiles.add(newJoin);

						AudioCore.concatAudioFiles(concat, newJoin, logger);

						// add the sprite data

						double start = AudioCore.getSoundDuration(join);
						double end = AudioCore.getSoundDuration(newJoin);

						logger.accept(file.getFullPath().toPortableString() + " [" + start + ", " + end + "]");

						join = newJoin;

						sprite.setStart(start);
						sprite.setEnd(end);
					}
					i++;
					monitor.worked(1);
				}

				// move resulting audio-sprite sound file to the workspace

				if (join != null) {
					java.nio.file.Path dstPath = eclipseFileToJavaPath(dstAudioFile);

					// the join var is pointing to the original file
					if (join.getFileName().toString().toLowerCase().endsWith(".wav")) {
						Files.copy(join, dstPath, StandardCopyOption.REPLACE_EXISTING);
					} else {
						AudioCore.convertAudioFile(join, dstPath, logger);
					}

					cancelFiles.add(dstPath);
					cancelFiles.add(join);
				}

				// save audio sprites model

				jsonAudioSpritesFile = dstDir.getFile(new Path(audioSpritesFileName));
				cancelFiles.add(eclipseFileToJavaPath(jsonAudioSpritesFile));

				{
					model.setModelFile(jsonAudioSpritesFile);
					// set the resources section of the model
					List<IFile> audioSpriteResources = new ArrayList<>();
					if (!concatFiles.isEmpty()) {
						for (String codec : new String[] { "wav", "ogg", "mp3" }) {
							audioSpriteResources.add(dstDir.getFile(new Path(audioSpritesName + "." + codec)));
						}
					}
					model.setResources(audioSpriteResources);
				}

				String spritesDef = model.toJSON().toString(2);
				logger.accept(spritesDef);

				{
					ByteArrayInputStream content = new ByteArrayInputStream(spritesDef.getBytes());
					if (jsonAudioSpritesFile.exists()) {
						jsonAudioSpritesFile.setContents(content, IResource.FORCE, new NullProgressMonitor());
					} else {
						jsonAudioSpritesFile.create(content, IResource.FORCE, new NullProgressMonitor());
					}
				}
			}

			// convert the resulting audio file to the different formats

			if (!concatFiles.isEmpty()) {

				java.nio.file.Path spriteAudioFile = eclipseFileToJavaPath(dstAudioFile);

				for (String codec : new String[] { "ogg", "mp3" }) {

					if (monitor.isCanceled()) {
						throw new CancellationException();
					}

					java.nio.file.Path otherFile = spriteAudioFile.resolveSibling(audioSpritesName + "." + codec);

					monitor.subTask("Generating " + otherFile.getFileName() + "...");

					Files.deleteIfExists(otherFile);
					AudioCore.convertAudioFile(spriteAudioFile, otherFile, logger);
					cancelFiles.add(otherFile);

					monitor.worked(1);
				}

				if (monitor.isCanceled()) {
					throw new CancellationException();
				}
			}

			for (IFile file : model.getResources()) {
				monitor.subTask("Making waves of " + file.getName() + "...");
				file.refreshLocal(IResource.DEPTH_ONE, monitor);
				AudioCore.removeSoundProperties(file);
				AudioCore.getSoundWavesFile(file);
				monitor.worked(1);
				AudioCore.getSoundDuration(file);
				monitor.worked(1);
			}

			return jsonAudioSpritesFile;
		} catch (Exception e) {
			for (java.nio.file.Path file : cancelFiles) {
				out.println("Cancel file " + file);
				Files.deleteIfExists(file);
			}
			if (!(e instanceof CancellationException)) {
				throw e;
			}
		} finally {
			for (java.nio.file.Path file : temporalFiles) {
				out.println("Delete temporal file " + file);
				Files.deleteIfExists(file);
			}
		}
		return jsonAudioSpritesFile;

	}

	public static double[][] createTimePartition(List<? extends AudioSprite> sprites) {
		double[][] partition = new double[sprites.size()][];
		for (int i = 0; i < sprites.size(); i++) {
			AudioSprite sprite = sprites.get(i);
			double[] tuple = partition[i] = new double[2];
			tuple[0] = sprite.getStart();
			tuple[1] = sprite.getEnd();
		}
		return partition;
	}
}
