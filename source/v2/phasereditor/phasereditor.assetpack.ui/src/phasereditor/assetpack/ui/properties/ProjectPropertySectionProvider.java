package phasereditor.assetpack.ui.properties;

import java.util.List;

import phasereditor.ui.properties.ExtensibleFormPropertyPage;
import phasereditor.ui.properties.FormPropertySection;
import phasereditor.ui.properties.IFormPropertySectionProvider;

public class ProjectPropertySectionProvider implements IFormPropertySectionProvider {

	public ProjectPropertySectionProvider() {
		// /
	}

	@Override
	public void createSections(ExtensibleFormPropertyPage page, List<FormPropertySection<?>> list) {
		list.add(new ImageFilePreviewPropertySection());
		list.add(new ManyImageFilePreviewPropertySection());
		list.add(new AudioFilePreviewPropertySection());
	}

}
