package phasereditor.inspect.core;

import org.eclipse.ui.IStartup;

public class Startup implements IStartup {

	@Override
	public void earlyStartup() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				InspectCore.getPhaserHelp();
			}
		}).start();
	}

}
