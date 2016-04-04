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
package phasereditor.ui;

import static java.lang.System.currentTimeMillis;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.swt.widgets.Display;

public abstract class Animation {
	private static ExecutorService _threads;
	protected int _fps;
	protected boolean _stopped;
	protected boolean _paused;

	static {
		_threads = Executors.newCachedThreadPool();
	}

	public Animation(int fps) {
		super();
		_fps = fps;
		_stopped = false;

	}

	public void start() {
		_stopped = false;

		_threads.execute(new Runnable() {

			@Override
			public void run() {
				Runnable action = new Runnable() {

					@Override
					public void run() {
						action();
					}
				};

				long lastTime = currentTimeMillis();
				try {
					while (!_stopped) {
						if (_paused) {
							Thread.sleep(100);
						} else {
							long time = currentTimeMillis() - lastTime;
							long target = 1000 / _fps;
							long delay = target - time;
							if (delay > 0) {
								Thread.sleep(delay);
								Display.getDefault().syncExec(action);
							}
							lastTime = currentTimeMillis();
						}
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}
		});

	}

	public void stop() {
		_stopped = true;
	}

	public void pause(boolean paused) {
		_paused = paused;
	}

	public boolean isPaused() {
		return _paused;
	}

	public int getFps() {
		return _fps;
	}

	public void setFps(int fps) {
		_fps = fps;
	}

	public abstract void action();
}
