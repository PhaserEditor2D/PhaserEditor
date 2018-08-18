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
package phasereditor.audio.core;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.out;
import static phasereditor.ui.PhaserEditorUI.eclipseFileToJavaPath;
import static phasereditor.ui.PhaserEditorUI.getExtensionFromFilename;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;
import org.json.JSONObject;
import org.lwjgl.openal.AL;

import com.badlogic.gdx.Audio;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.backends.lwjgl.audio.OpenALAudio;
import com.badlogic.gdx.backends.lwjgl.audio.OpenALMusic;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxNativesLoader;

import phasereditor.inspect.core.InspectCore;
import phasereditor.project.core.ProjectCore;
import phasereditor.ui.FileUtils;

/**
 * We should call this class MediaCore, and the plugin phasereditor.media.core
 * 
 * @author arian
 *
 */
public class AudioCore {
	public static final String PLUGIN_ID = Activator.PLUGIN_ID;

	private static final QualifiedName WAVEFORM_FILENAME_KEY = new QualifiedName("phasereditor.audio.core",
			"waveform-file");
	private static final QualifiedName DURATION_KEY = new QualifiedName("phasereditor.audio.core", "duration");

	private static final String[] SUPPORTED_VIDEO_EXTENSIONS = { "mp4", "ogv", "webm", "flv", "wmv", "avi", "mpg" };

	private static final QualifiedName SNAPSHOT_FILENAME_KEY = new QualifiedName("phasereditor.audio.core",
			"snapshot-file");

	public static String[] SUPPORTED_SOUND_EXTENSIONS = { "wav", "ogg", "mp3" };

	private static Path _silencePath;

	private static Audio _audio;
	static List<OpenALMusic> _musicsToUpdate;
	static List<Runnable> _musicActionsToUpdate;

	static {
		initGdxAudio();
	}

	private static void initGdxAudio() {
		GdxNativesLoader.disableNativesLoading = true;
		_musicsToUpdate = new ArrayList<>();
		_musicActionsToUpdate = new ArrayList<>();

		// music loop

		Thread th = new Thread("Phaser Editor music loop") {
			@Override
			public void run() {
				while (true) {
					try {
						sleep(100);
					} catch (InterruptedException e) {
						// nothing
					}
					synchronized (_musicsToUpdate) {
						for (OpenALMusic music : _musicsToUpdate) {
							synchronized (music) {
								if (music.isPlaying()) {
									music.update();
								}
							}
						}
					}

					synchronized (_musicActionsToUpdate) {
						for (Runnable action : _musicActionsToUpdate) {
							try {
								action.run();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		};
		th.setDaemon(true);
		th.start();
	}

	static void disposeGdxAudio() {
		synchronized (_musicsToUpdate) {
			for (OpenALMusic m : _musicsToUpdate) {
				synchronized (m) {
					m.dispose();
				}
			}
		}

		AL.destroy();
	}

	private static Audio getAudio() {
		if (_audio == null) {
			_audio = new OpenALAudio();
		}
		return _audio;
	}

	public static Music createGdxMusic(FileHandle file) throws Exception {
		OpenALMusic music = (OpenALMusic) getAudio().newMusic(file);
		synchronized (_musicsToUpdate) {
			_musicsToUpdate.add(music);
		}
		return music;
	}

	public static void disposeGdxMusic(Music music) {
		music.dispose();
		synchronized (_musicsToUpdate) {
			_musicsToUpdate.remove(music);
		}
	}

	static class MemoryFileHandle extends FileHandle {
		private byte[] _bytes;

		public MemoryFileHandle(Path path) throws IOException {
			super(path.toFile());
			long t = currentTimeMillis();
			_bytes = Files.readAllBytes(path);
			out.println("read " + path.getFileName() + " in " + (currentTimeMillis() - t) + " ms");
		}

		@Override
		public InputStream read() {
			return new ByteArrayInputStream(_bytes);
		}
	}

	public static Music createGdxMusic(IFile file) throws Exception {
		try {
			Path path = eclipseFileToJavaPath(file);
			if (Files.size(path) > 1024 * 1024 * 50) {
				return createGdxMusic(new FileHandle(path.toFile()));
			}
			return createGdxMusic(new MemoryFileHandle(path));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void addMusicUpdateAction(Runnable action) {
		synchronized (_musicsToUpdate) {
			_musicActionsToUpdate.add(action);
		}
	}

	public static void removeMusicUpdateAction(Runnable action) {
		synchronized (_musicsToUpdate) {
			_musicActionsToUpdate.remove(action);
		}
	}

	public static boolean isSupportedAudio(IFile file) {
		return isSupportedAudio(file.getLocation().toFile());
	}

	public static boolean isSupportedAudio(File file) {
		for (String ext : SUPPORTED_SOUND_EXTENSIONS) {
			if (file.getName().toLowerCase().endsWith(ext)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isSupportedVideo(IFile file) {
		return isSupportedVideo(file.getLocation().toFile());
	}

	public static boolean isSupportedVideo(File file) {
		for (String ext : SUPPORTED_VIDEO_EXTENSIONS) {
			if (file.getName().toLowerCase().endsWith(ext)) {
				return true;
			}
		}
		return false;
	}

	public static void concatAudioFiles(List<IFile> files, IFile dstFile, Consumer<String> logger) throws Exception {
		List<Path> files2 = new ArrayList<>();
		for (IFile file : files) {
			files2.add(eclipseFileToJavaPath(file));
		}

		Path dstFile2 = eclipseFileToJavaPath(dstFile);

		concatAudioFiles(files2, dstFile2, logger);
	}

	public static void concatAudioFiles(List<Path> files, Path dstFile, Consumer<String> logger) throws Exception {
		Path tmpDir = Files.createTempDirectory("PhaserEditor_");

		try {
			logger.accept("Set temp dir " + tmpDir + "\n");

			StringBuilder sb = new StringBuilder();

			int i = 0;
			for (Path file : files) {
				String filename = "audio-" + i++ + ".wav";
				sb.append("file '" + filename + "'\n");

				Path outFile = tmpDir.resolve(filename);

				logger.accept("Convert " + file.getFileName() + " to " + outFile);

				convertAudioFile(file, outFile, logger);
			}

			logger.accept("\nWrite list file:\n" + sb + "\n");

			Files.write(tmpDir.resolve("list.txt"), sb.toString().getBytes());

			logger.accept("Delete " + dstFile + "\n");
			Files.deleteIfExists(dstFile);

			logger.accept("Output " + dstFile);

			ProcessBuilder pb = createFFMpegProcessBuilder("-v", "warning", "-hide_banner", "-f", "concat", "-i",
					"list.txt", "-c", "copy", dstFile.toString());
			pb.directory(tmpDir.toFile());

			logger.accept(Arrays.toString(pb.command().toArray()) + "\n");

			Process proc = pb.start();
			FileUtils.readStream(proc.getErrorStream(), logger);

			int exitValue = proc.waitFor();
			if (exitValue != 0) {
				throw new IOException("FFMpeg termination exitValue " + exitValue);
			}

			logger.accept("\ndone\n\n");

		} finally {
			Files.walk(tmpDir).forEach(p -> {
				try {
					if (!Files.isDirectory(p)) {
						logger.accept("Delete " + p);
						Files.delete(p);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			Files.delete(tmpDir);
		}
	}

	public static void convertAudioFile(IFile inFile, IFile outFile, Consumer<String> logger) throws Exception {
		convertAudioFile(eclipseFileToJavaPath(inFile), eclipseFileToJavaPath(outFile), logger);
	}

	public static void convertAudioFile(Path inFile, Path outFile, Consumer<String> logger) throws Exception {
		String ext1 = getExtensionFromFilename(inFile.getFileName().toString());
		String ext2 = getExtensionFromFilename(outFile.getFileName().toString());

		if (ext1.equals(ext2)) {
			// if they are of the same extension, then assume they have the same
			// format so a conversion is not needed, a copy is more than fine.
			Files.copy(inFile, outFile, StandardCopyOption.REPLACE_EXISTING);
			return;
		}

		ProcessBuilder pb = createFFMpegProcessBuilder("-v", "warning", "-hide_banner", "-i",
				inFile.toAbsolutePath().toString(), outFile.toAbsolutePath().toString());

		Process proc = pb.start();

		FileUtils.readStream(proc.getInputStream(), logger);
		FileUtils.readStream(proc.getErrorStream(), logger);

		int exitValue = proc.waitFor();

		if (exitValue != 0) {
			throw new IOException("FFMpeg termination exitValue " + exitValue);
		}
	}

	public static double computeAudioDuration(IFile file) {
		return getSoundDuration(eclipseFileToJavaPath(file));
	}

	public static double getSoundDuration(Path file) {
		String path = file.toFile().getAbsolutePath();
		ProcessBuilder pb = AudioCore.createFFProbeProcessBuilder("-v", "quiet", "-hide_banner", "-show_format",
				"-print_format", "json", path);
		Process proc;
		try {
			proc = pb.start();
			String output = FileUtils.readStream(proc.getInputStream());
			int exitValue = proc.waitFor();
			if (exitValue != 0) {
				out.println(Arrays.toString(pb.command().toArray()));
				throw new IOException("FFMpeg termination exitValue " + exitValue);
			}
			output = output.replace("/n", "\n");
			JSONObject obj = new JSONObject(output);
			obj = obj.getJSONObject("format");
			double duration = obj.getDouble("duration");
			return duration;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static java.nio.file.Path getSilenceAudioFile() {
		if (_silencePath == null) {
			try {
				URL url = new URL("platform:/plugin/" + PLUGIN_ID + "/silence.wav");
				url = FileLocator.toFileURL(url);
				File file = new File(url.getFile());
				_silencePath = file.toPath().normalize();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return _silencePath;
	}

	public static double getSilenceAudioFileDuration() {
		// keep this always updated
		return 0.5;
	}

	public static ProcessBuilder createFFMpegProcessBuilder(String... args) {
		return InspectCore.createProcessBuilder("ffmpeg/ffmpeg", args);
	}

	public static ProcessBuilder createFFPlayProcessBuilder(String... args) {
		return InspectCore.createProcessBuilder("ffmpeg/ffplay", args);
	}

	public static ProcessBuilder createFFProbeProcessBuilder(String... args) {
		return InspectCore.createProcessBuilder("ffmpeg/ffprobe", args);
	}

	public static void removeSoundProperties(IFile file) {
		try {
			file.setPersistentProperty(WAVEFORM_FILENAME_KEY, null);
			file.setPersistentProperty(DURATION_KEY, null);
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized static Path getSoundWavesFile(IFile file) {
		return getSoundWavesFile(file, true);
	}

	public synchronized static Path getSoundWavesFile(IFile file, boolean forceMake) {
		if (file == null) {
			return null;
		}
		
		try {
			String filename = file.getPersistentProperty(WAVEFORM_FILENAME_KEY);
			Path dir = ProjectCore.getUserCacheFolder().resolve("waves");
			Path path;
			if (filename == null) {
				filename = UUID.randomUUID().toString() + ".png";
				path = dir.resolve(filename);
				file.setPersistentProperty(WAVEFORM_FILENAME_KEY, filename);
			} else {
				path = dir.resolve(filename);
			}

			if (forceMake) {
				if (!Files.exists(path)) {
					makeSoundWaves(file, path);
				}
			}

			return path;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static void makeSoundWaves(IFile file, Path path) throws IOException {
		out.println("Make waves " + file);
		Files.createDirectories(path.getParent());

		String soundPath = eclipseFileToJavaPath(file).toString();

		ProcessBuilder pb = createFFMpegProcessBuilder("-i", soundPath, "-lavfi",
				"showwavespic=split_channels=1:s=800x600", path.toString());
		pb.start();
	}

	public static void makeSoundWavesAndMetadata(IResourceDelta projectDelta) {
		try {
			projectDelta.accept(new IResourceDeltaVisitor() {

				@Override
				public boolean visit(IResourceDelta delta) throws CoreException {
					IResource resource = delta.getResource();
					if (resource instanceof IFile) {
						IFile file = (IFile) resource;
						if (resource.exists() && isSupportedAudio(file)) {
							if (delta.getKind() == IResourceDelta.CHANGED) {
								removeSoundProperties(file);
							}
							getSoundWavesFile(file);
							getSoundDuration(file);
						}
					}
					return true;
				}
			});
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	public synchronized static double getSoundDuration(IFile file) {
		try {
			String value = file.getPersistentProperty(DURATION_KEY);

			if (value == null) {
				double duration = computeAudioDuration(file);
				value = Double.toString(duration);
				file.setPersistentProperty(DURATION_KEY, value);
				return duration;
			}

			return Double.parseDouble(value);

		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}

	protected static void removeVideoProperties(IFile file) {
		try {
			file.setPersistentProperty(SNAPSHOT_FILENAME_KEY, null);
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized static Path getVideoSnapshotFile(IFile file) {
		return getVideoSnapshotFile(file, true);
	}

	public synchronized static Path getVideoSnapshotFile(IFile file, boolean forceMake) {
		if (file == null) {
			return null;
		}

		try {
			String filename = file.getPersistentProperty(SNAPSHOT_FILENAME_KEY);
			
			Path dir = ProjectCore.getUserCacheFolder().resolve("snapshots");
			Path path;
			if (filename == null) {
				filename = UUID.randomUUID().toString() + ".jpg";
			}
			path = dir.resolve(filename);

			if (forceMake) {
				if (!Files.exists(path)) {
					makeVideoSnapshot(file, path);
				}
			}

			file.setPersistentProperty(SNAPSHOT_FILENAME_KEY, filename);

			return path;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static void makeVideoSnapshot(IFile file, Path path) throws IOException {
		Files.createDirectories(path.getParent());

		String videoPath = eclipseFileToJavaPath(file).toString();
		out.println("Make video screenshot " + file);
		ProcessBuilder pb = createFFMpegProcessBuilder("-hide_banner", "-loglevel", "0", "-ss", "00:00:01", "-i",
				videoPath, "-vframes", "1", "-vf", "scale=128:-1", path.toAbsolutePath().toString());
		Process proc = pb.start();
		try {
			proc.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void logError(Exception e) {
		e.printStackTrace();
		StatusManager.getManager().handle(new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage(), e));
	}

}
