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
package phasereditor.audio.ui;

import static phasereditor.ui.PhaserEditorUI.paintPreviewMessage;
import static phasereditor.ui.PhaserEditorUI.swtRun;

import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.wb.swt.ResourceManager;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Music.OnCompletionListener;

import phasereditor.audio.core.AudioCore;

public class GdxMusicControl extends Composite implements DisposeListener, MouseMoveListener, MouseTrackListener {
	private Music _music;
	protected boolean _playing;
	private OnCompletionListener _musicListener;
	private Canvas _canvas;
	private Image _wavesImage;
	private IFile _file;
	private Label _label;
	private double _duration;
	private ImageDescriptor _imgStop;
	private ImageDescriptor _imgPlay;
	private double[][] _partition;
	private int _partitionSelection = -1;
	private double _startTime = -1;
	private double _endTime = -1;
	private int _cursorX = -1;
	private boolean _paintTimeCursor;
	protected final ToolBar _toolBar;
	private Action _playAction;
	private String _errorMessage;

	/**
	 * Create the composite.
	 * 
	 * @param parent
	 * @param style
	 */
	public GdxMusicControl(Composite parent, int style) {
		super(parent, style);

		GridLayout gridLayout = new GridLayout(1, false);
		setLayout(gridLayout);

		_canvas = new Canvas(this, SWT.DOUBLE_BUFFERED);
		_canvas.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				paintCanvas(e);
			}
		});
		GridData gd_canvas = new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1);
		gd_canvas.heightHint = 50;
		_canvas.setLayoutData(gd_canvas);

		Composite composite = new Composite(this, SWT.NONE);
		GridLayout gl_composite = new GridLayout(2, false);
		gl_composite.marginWidth = 0;
		gl_composite.marginHeight = 0;
		composite.setLayout(gl_composite);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

		_toolBar = new ToolBar(composite, SWT.FLAT | SWT.RIGHT);

		_label = new Label(composite, SWT.NONE);
		_label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		createActions();

		afterCreateWidgets();

	}

	private void createActions() {
		_imgStop = ResourceManager.getPluginImageDescriptor("phasereditor.ui", "icons/control_stop.png");
		_imgPlay = ResourceManager.getPluginImageDescriptor("phasereditor.ui", "icons/control_play.png");

		_playAction = new Action("play", _imgPlay) {
			@Override
			public void run() {
				playOrStop();
			}
		};
	}

	protected void paintCanvas(PaintEvent e) {
		Image img = _wavesImage;
		Rectangle canvasRect = _canvas.getBounds();
		canvasRect.x = 0;
		canvasRect.y = 0;
		GC gc = e.gc;

		if (_music == null) {
			String msg = _errorMessage == null ? "(no audio)" : _errorMessage;
			paintPreviewMessage(gc, canvasRect, msg);
		} else {

			Display display = getDisplay();

			gc.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
			gc.fillRectangle(canvasRect);

			Color grayColor = display.getSystemColor(SWT.COLOR_DARK_GRAY);

			if (img == null) {
				paintPreviewMessage(gc, canvasRect, "waves image not found");
			} else {
				Rectangle imgRect = img.getBounds();
				gc.drawImage(img, 0, 0, imgRect.width, imgRect.height, 0, 0, canvasRect.width, canvasRect.height);
			}

			// partition

			if (_partition != null && _duration > 0) {
				Color c1 = grayColor;
				Color c2 = display.getSystemColor(SWT.COLOR_DARK_BLUE);
				int i = 0;
				gc.setAlpha(90);
				for (double[] tuple : _partition) {
					double start = tuple[0];
					double end = tuple[1];
					int x1 = (int) (start / _duration * canvasRect.width);
					int x2 = (int) (end / _duration * canvasRect.width);
					gc.setBackground(i == _partitionSelection ? c2 : c1);
					gc.fillRectangle(x1, 0, x2 - x1, canvasRect.height);
					i++;
				}
				gc.setAlpha(255);
			}

			if (_duration > 0) {

				gc.setForeground(display.getSystemColor(SWT.COLOR_RED));

				// play-line

				if (_playing) {
					float position = _music.getPosition();
					if (_endTime < 0 || position <= _endTime) {
						double percent = position / _duration;
						int x = (int) (canvasRect.width * percent);
						gc.drawLine(x, 0, x, canvasRect.height);
					}
				}

				// cursor line

				if (_paintTimeCursor && _cursorX >= 0) {
					double time = (double) _cursorX / (double) canvasRect.width * _duration;

					gc.drawLine(_cursorX, 0, _cursorX, canvasRect.height);

					time = ((int) (time * 100)) / (double) 100;
					gc.setBackground(grayColor);
					gc.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
					gc.drawText(Double.toString(time), _cursorX + 5, 5);
				}
			}

		}
	}

	private void afterCreateWidgets() {
		addDisposeListener(this);

		_canvas.addMouseMoveListener(this);
		_canvas.addMouseTrackListener(this);

		ToolBarManager manager = new ToolBarManager(_toolBar);
		manager.add(_playAction);
		manager.update(true);
	}

	protected void playOrStop() {
		if (_music == null) {
			return;
		}

		if (_playing) {
			_music.stop();
		} else {
			_music.play();

			if (_startTime > 0) {
				_music.setPosition((float) _startTime);
			}

			_music.setOnCompletionListener(_musicListener);
		}
		_playing = !_playing;

		updateButton();
		_canvas.redraw();
	}

	public Music getMusic() {
		return _music;
	}

	public Canvas getCanvas() {
		return _canvas;
	}

	public IFile getFile() {
		return _file;
	}

	public void setTimePartition(double[][] partition) {
		_partition = partition;
		_canvas.redraw();
	}

	public double[][] getTimePartition() {
		return _partition;
	}

	public void setPaintTimeCursor(boolean paintTimeCursor) {
		_paintTimeCursor = paintTimeCursor;
	}

	public boolean isPaintTimeCursor() {
		return _paintTimeCursor;
	}

	public void setTimePartitionSelection(int partitionSelection) {
		_partitionSelection = partitionSelection;

		if (partitionSelection == -1) {
			_startTime = _endTime = -1;
		} else {
			double[] tuple = _partition[partitionSelection];
			_startTime = tuple[0];
			_endTime = tuple[1];
		}
		_canvas.redraw();
	}

	public int getTimePartitionSelection() {
		return _partitionSelection;
	}

	public double getDuration() {
		return _duration;
	}

	public void load(IFile file) {
		_duration = 0;

		if (_music != null) {
			disposeMusic();
		}

		_playing = false;
		_file = file;

		_errorMessage = null;
		if (file != null) {
			try {
				_music = AudioCore.createGdxMusic(file);
			} catch (Exception e) {
				e.printStackTrace();
				_errorMessage = e.getMessage();
			}
		}

		_musicListener = new OnCompletionListener() {

			@Override
			public void onCompletion(Music music) {
				_playing = false;
				try {
					swtRun(new Runnable() {

						@Override
						public void run() {
							updateButton();
							getCanvas().redraw();
						}
					});
				} catch (SWTException e) {
					// nothing
				}
			}
		};
		AudioCore.addMusicUpdateAction(this::updateProgress);

		if (file != null) {
			loadWavsImage(3);
			_duration = AudioCore.getSoundDuration(file);
			_canvas.redraw();
		}

		updateButton();
	}

	protected void loadWavsImage(int n) {
		if (_file == null) {
			return;
		}

		Path soundPath = AudioCore.getSoundWavesFile(_file);
		if (Files.exists(soundPath)) {
			ImageLoader loader = new ImageLoader();
			try {
				ImageData[] dataArray = loader.load(soundPath.toString());
				ImageData data = dataArray[0];
				data.transparentPixel = 0;
				_wavesImage = new Image(getDisplay(), data);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (n >= 0) {
			// try later
			new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					loadWavsImage(n - 1);
					swtRun(getCanvas()::redraw);
				}
			}).start();
		}
	}

	protected void updateButton() {
		if (_music == null) {
			_label.setText("");
			if (!_toolBar.isDisposed()) {
				_playAction.setEnabled(false);
			}
		} else {
			_label.setText(_file.getName() + " (" + _duration + " secs)");
			if (!_toolBar.isDisposed()) {
				_playAction.setEnabled(true);
			}
		}

		if (!_toolBar.isDisposed()) {
			if (_playing) {
				_playAction.setImageDescriptor(_imgStop);
			} else {
				_playAction.setImageDescriptor(_imgPlay);
			}
		}
	}

	public void disposeMusic() {
		Music music = getMusic();
		if (music != null) {
			AudioCore.disposeGdxMusic(music);
			AudioCore.removeMusicUpdateAction(this::updateProgress);
			_music = null;
		}
	}

	@Override
	public void widgetDisposed(DisposeEvent e) {
		disposeMusic();
		if (_wavesImage != null) {
			_wavesImage.dispose();
		}
	}

	private void updateProgress() {
		if (_playing) {
			if (_endTime > 0 && _music != null && _music.getPosition() >= _endTime) {
				stop();
			}
			swtRun(_canvas::redraw);
		}
	}

	public void stop() {
		if (_music != null) {
			_music.stop();
			_musicListener.onCompletion(_music);
		}
	}

	public void redrawCanvas() {
		_canvas.redraw();
	}

	@Override
	public void mouseMove(MouseEvent e) {
		_cursorX = e.x;
		if (_paintTimeCursor) {
			_canvas.redraw();
		}
	}

	@Override
	public void mouseEnter(MouseEvent e) {
		mouseMove(e);
	}

	@Override
	public void mouseExit(MouseEvent e) {
		_cursorX = -1;
		_canvas.redraw();
	}

	@Override
	public void mouseHover(MouseEvent e) {
		// nothing
	}
}
