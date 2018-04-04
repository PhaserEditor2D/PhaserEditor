package phasereditor.chains.core;

import org.eclipse.ui.IStartup;

public class Startup implements IStartup {

	@Override
	public void earlyStartup() {
		ChainsCore.getChainsModel();
	}

}
