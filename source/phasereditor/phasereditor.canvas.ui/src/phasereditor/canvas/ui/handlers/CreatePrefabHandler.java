package phasereditor.canvas.ui.handlers;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.util.UUID;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;
import org.json.JSONObject;

import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.CanvasModel;
import phasereditor.canvas.core.CanvasModelFactory;
import phasereditor.canvas.core.CanvasType;
import phasereditor.canvas.core.EditorSettings;
import phasereditor.canvas.core.GroupModel;
import phasereditor.canvas.core.WorldModel;
import phasereditor.canvas.core.codegen.CanvasCodeGeneratorProvider;
import phasereditor.canvas.ui.CanvasUI;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.shapes.IObjectNode;
import phasereditor.project.core.ProjectCore;
import phasereditor.project.core.codegen.SourceLang;

public class CreatePrefabHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
		IFile canvasFile = editor.getEditorInputFile();
		IProject project = canvasFile.getProject();
		IContainer webFolder = ProjectCore.getWebContentFolder(project);
		IWorkspaceRoot root = project.getWorkspace().getRoot();

		SaveAsDialog dlg = new SaveAsDialog(HandlerUtil.getActiveShell(event)) {
			@Override
			protected Control createDialogArea(Composite parent) {
				Control area = super.createDialogArea(parent);

				try {
					Field field = SaveAsDialog.class.getDeclaredField("resourceGroup");
					field.setAccessible(true);
					Object group = field.get(this);

					field = group.getClass().getDeclaredField("containerGroup");
					field.setAccessible(true);
					group = field.get(group);

					field = group.getClass().getDeclaredField("treeViewer");
					field.setAccessible(true);
					TreeViewer viewer = (TreeViewer) field.get(group);
					getShell().getDisplay().asyncExec(() -> {
						viewer.setInput(webFolder);
						viewer.setSelection(new StructuredSelection(canvasFile.getParent()));
					});
				} catch (Exception e) {
					e.printStackTrace();
				}

				return area;
			}
		};

		if (dlg.open() == Window.OK) {
			IPath newPath = dlg.getResult();

			newPath = newPath.removeFileExtension().addFileExtension("canvas");

			IFile file = root.getFile(newPath);

			if (file.exists()) {
				MessageDialog.openError(null, "Create Prefab", "Cannot create the prefab, the file already exists.");
				return null;
			}

			if (!file.getParent().exists()) {
				MessageDialog.openError(null, "Create Prefab",
						"Cannot create the prefab, the selected folder does not exists.");
				return null;
			}

			IObjectNode node = (IObjectNode) HandlerUtil.getCurrentStructuredSelection(event).getFirstElement();
			BaseObjectModel selModel = node.getModel();

			CanvasModel newModel = new CanvasModel(null);

			CanvasType type = selModel instanceof GroupModel ? CanvasType.GROUP : CanvasType.SPRITE;
			newModel.setType(type);

			String name = file.getFullPath().removeFileExtension().lastSegment();

			EditorSettings settings = newModel.getSettings();
			settings.setBaseClass(CanvasCodeGeneratorProvider.getDefaultBaseClassFor(type));
			SourceLang lang = node.getControl().getCanvas().getEditor().getModel().getSettings().getLang();
			settings.setLang(lang);
			settings.setClassName(name);
			
			WorldModel world = newModel.getWorld();
			newModel.setFile(file);
			world.setEditorName("root");

			if (type == CanvasType.GROUP) {
				GroupModel groupModel = (GroupModel) selModel;
				groupModel = (GroupModel) CanvasModelFactory.createModel(world, groupModel.toJSON(false));
				groupModel.setEditorName("group");
				groupModel.setX(0);
				groupModel.setY(0);
				groupModel.trim();
				world.addChild(groupModel);
			} else {
				JSONObject json = selModel.toJSON(false);
				BaseObjectModel cModel = CanvasModelFactory.createModel(world, json);
				cModel.setX(0);
				cModel.setY(0);
				world.addChild(cModel);
			}

			world.setId(UUID.randomUUID().toString());
			world.walk(m -> m.setId(UUID.randomUUID().toString()));

			JSONObject obj = new JSONObject();
			newModel.write(obj, false);

			WorkspaceJob job = new WorkspaceJob("Create Prefab") {

				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
					try {
						file.create(new ByteArrayInputStream(obj.toString(2).getBytes()), true, monitor);
					} catch (Exception e) {
						CanvasUI.logError(e);
						return new Status(IStatus.ERROR, CanvasUI.PLUGIN_ID, e.getMessage(), e);
					}
					return Status.OK_STATUS;
				}
			};

			job.addJobChangeListener(new JobChangeAdapter() {
				@Override
				public void done(IJobChangeEvent e) {
					HandlerUtil.getActiveShell(event).getDisplay().asyncExec(() -> {
						try {
							CanvasEditor editor2 = (CanvasEditor) IDE
									.openEditor(HandlerUtil.getActiveWorkbenchWindow(event).getActivePage(), file);
							editor2.generateCode();
						} catch (PartInitException e1) {
							CanvasUI.logError(e1);
						}
					});
				}
			});
			job.schedule();
		}

		return null;
	}

}
