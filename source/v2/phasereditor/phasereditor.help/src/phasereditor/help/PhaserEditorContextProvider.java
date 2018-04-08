package phasereditor.help;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.help.AbstractContextProvider;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.IContext;

public class PhaserEditorContextProvider extends AbstractContextProvider {

	private Map<String, String> MAP = new HashMap<String, String>() {
		private static final long serialVersionUID = 1L;

		{
			//TODO: #RemovingWST
			// put("org.eclipse.wst.jsdt.ui.java_editor_context", "phasereditor.help.javascripteditor");
		}
	};

	public PhaserEditorContextProvider() {
	}

	@Override
	public IContext getContext(String id, String locale) {
		String id2 = MAP.get(id);

		if (id2 == null) {
			return null;
		}

		return HelpSystem.getContext(id2);
	}

	@Override
	public String[] getPlugins() {
		//TODO: #RemovingWST
		// return new String[] { "org.eclipse.wst.jsdt.ui" };
		return new String[] {  };
	}

}
