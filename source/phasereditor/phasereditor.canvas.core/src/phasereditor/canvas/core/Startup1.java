package phasereditor.canvas.core;

import static java.lang.System.out;

import org.eclipse.ui.IStartup;

public class Startup1 implements IStartup {

	@Override
	public void earlyStartup() {
		out.println("Starting Canvas core.");
	}

}
