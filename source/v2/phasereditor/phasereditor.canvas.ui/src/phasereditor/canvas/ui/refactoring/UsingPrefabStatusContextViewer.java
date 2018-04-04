package phasereditor.canvas.ui.refactoring;

import java.nio.file.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.ui.refactoring.IStatusContextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import phasereditor.canvas.ui.CanvasUI;
import phasereditor.ui.ImageCanvas;

public class UsingPrefabStatusContextViewer implements IStatusContextViewer {

	private ImageCanvas _control;

	@Override
	public void createControl(Composite parent) {
		_control = new ImageCanvas(parent, SWT.NONE);
	}

	@Override
	public Control getControl() {
		return _control;
	}

	@Override
	public void setInput(RefactoringStatusContext input) {
		CanvasFileRefactoringStatusContext context = (CanvasFileRefactoringStatusContext) input;
		IFile clientFile = context.getCorrespondingElement();
		Path screenshot = CanvasUI.getCanvasScreenshotFile(clientFile, true);
		_control.setImageFile(screenshot.toFile().getAbsolutePath());
	}

}
