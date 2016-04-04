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

import static java.lang.System.out;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.json.JSONObject;

import phasereditor.ui.FileUtils;

@Deprecated
public class FFMpeg {

	private static ExecutorService _pool = Executors.newCachedThreadPool();

	private File _file;
	private IPlaybackListener _listener;
	private Process _proc;

	protected double _duration;
	protected long _bitRate;

	private boolean _infoReady;

	public interface IPlaybackListener {
		public void progress(int percent);

		public void started();

		public void stopped();
	}

	public FFMpeg(File file, IPlaybackListener listener) {
		_file = file;
		_listener = listener;
		_infoReady = false;
		requestInfo(null);
	}

	public double getDuration() {
		return _duration;
	}

	public long getBitRate() {
		return _bitRate;
	}

	public File getFile() {
		return _file;
	}

	public Process getProc() {
		return _proc;
	}

	public void play() {
		if (!checkFileExists()) {
			return;
		}
		play(-1, -1);
	}

	public void play(double start, double end) {
		ProcessBuilder pb;
		if (start == -1) {
			// ffplay -i [file] -loglevel quiet -autoexit -nodisp
			pb = FileUtils.createProcessBuilder(AudioCore.PLUGIN_ID, "ffmpeg/ffplay", "-i", _file.getAbsolutePath(),
					"-loglevel", "quiet", "-autoexit", "-nodisp");
		} else {
			// ffplay -i [file] -loglevel quiet -autoexit -nodisp -ss [start] -
			// t[end - start]
			pb = AudioCore.createFFPlayProcessBuilder("-i", _file.getAbsolutePath(), "-ss", Double.toString(start),
					"-t", Double.toString(end - start), "-loglevel", "quiet", "-autoexit", "-nodisp");
		}

		_pool.execute(new Runnable() {

			@SuppressWarnings("synthetic-access")
			@Override
			public void run() {
				try {
					_proc = pb.start();
					if (_listener != null) {
						_listener.started();
					}
					_proc.waitFor();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					stop();
				}
			}
		});
	}

	public void stop() {
		if (_proc != null) {
			_proc.destroy();
			_proc = null;
			if (_listener != null) {
				_listener.stopped();
			}
		}
	}

	public boolean isRunning() {
		return _proc != null && _proc.isAlive();
	}

	public boolean isInfoReady() {
		return _infoReady;
	}

	public void requestInfo(Consumer<JSONObject> callback) {
		_pool.execute(new Runnable() {

			@Override
			public void run() {
				File file = getFile();
				String path = file.getAbsolutePath();
				ProcessBuilder pb = AudioCore.createFFProbeProcessBuilder("-v", "quiet", "-hide_banner", "-show_format",
						"-print_format", "json", path);
				Process proc;
				try {
					proc = pb.start();
					String output = FileUtils.readStream(proc.getInputStream());
					output = output.replace("/n", "\n");
					JSONObject obj = new JSONObject(output);
					obj = obj.getJSONObject("format");
					updateInfo(obj);
					if (callback != null) {
						callback.accept(obj);
					}
				} catch (Exception e) {
					out.println("Exec: " + Arrays.toString(pb.command().toArray()));
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			}
		});
	}

	private boolean checkFileExists() {
		File file = getFile();
		if (!file.exists()) {
			Display display = Display.getDefault();
			display.asyncExec(() -> {
				MessageDialog.openError(display.getActiveShell(), "FFMpeg", "File not found: " + file);
			});
			return false;
		}
		return true;
	}

	public interface ISampleConsumer {
		public void sampleDecoded(byte sample);
	}

	@SuppressWarnings("unused")
	public void sample(ISampleConsumer consumer) throws IOException {
		/*
		 * String path = _file.getAbsolutePath(); ProcessBuilder pb =
		 * AudioCore.createFFMpegProcessBuilder("-i", path, "-ac", "1",
		 * "-filter:a", "aresample=8008", "-map", "0:a", "-c:a", "pcm_s8", "-f",
		 * "data", "-"); Process proc = pb.start(); try (InputStream input =
		 * proc.getInputStream()) { byte[] buff = new byte[1024]; int n; while
		 * ((n = input.read(buff)) != -1) { for (int i = 0; i < n; i++) {
		 * consumer.sampleDecoded(buff[i]); } } }
		 */
	}

	protected synchronized void updateInfo(JSONObject t) {
		_duration = t.getDouble("duration");
		_bitRate = t.getLong("bit_rate");
		_infoReady = true;
	}
}
