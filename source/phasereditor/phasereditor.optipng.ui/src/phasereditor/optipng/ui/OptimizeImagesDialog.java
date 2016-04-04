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
package phasereditor.optipng.ui;

import static java.lang.System.out;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import phasereditor.optipng.core.OptiPNGCore;

public class OptimizeImagesDialog extends Dialog {
	private Table _table;
	List<IResource> _selection;
	Label _labelTitle;
	TableViewer _tableViewer;
	HashMap<IResource, String> _oldSizeMap;
	HashMap<IResource, String> _newSizeMap;
	HashMap<IResource, String> _reductionMap;

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public OptimizeImagesDialog(Shell parentShell) {
		super(parentShell);
	}

	/**
	 * Create contents of the dialog.
	 * 
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout gridLayout = (GridLayout) container.getLayout();
		gridLayout.marginTop = 5;
		gridLayout.marginRight = 5;
		gridLayout.marginLeft = 5;
		gridLayout.marginBottom = 5;
		gridLayout.marginWidth = 5;
		gridLayout.marginHeight = 5;

		_labelTitle = new Label(container, SWT.NONE);
		_labelTitle.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		_labelTitle.setText("Running OptiPNG on...");

		_tableViewer = new TableViewer(container, SWT.BORDER | SWT.FULL_SELECTION);
		_table = _tableViewer.getTable();
		_table.setLinesVisible(true);
		_table.setHeaderVisible(true);
		_table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		TableViewerColumn tableViewerColumn = new TableViewerColumn(_tableViewer, SWT.NONE);
		tableViewerColumn.setLabelProvider(new ColumnLabelProvider() {
			WorkbenchLabelProvider _provider = new WorkbenchLabelProvider();

			@Override
			public Image getImage(Object element) {
				return _provider.getImage(element);
			}

			@Override
			public String getText(Object element) {
				IResource res = (IResource) element;
				IPath path = res.getFullPath();
				return path.lastSegment();
			}
		});
		TableColumn tblclmnResource = tableViewerColumn.getColumn();
		tblclmnResource.setWidth(171);
		tblclmnResource.setText("Filename");

		TableViewerColumn tableViewerColumn_1 = new TableViewerColumn(_tableViewer, SWT.NONE);
		tableViewerColumn_1.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return _oldSizeMap.get(element);
			}
		});
		TableColumn tblclmnOrigSize = tableViewerColumn_1.getColumn();
		tblclmnOrigSize.setWidth(110);
		tblclmnOrigSize.setText("Orig. Size");

		TableViewerColumn tableViewerColumn_2 = new TableViewerColumn(_tableViewer, SWT.NONE);
		tableViewerColumn_2.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return _newSizeMap.get(element);
			}
		});
		TableColumn tblclmnNewSize = tableViewerColumn_2.getColumn();
		tblclmnNewSize.setWidth(105);
		tblclmnNewSize.setText("New Size");

		TableViewerColumn tableViewerColumn_3 = new TableViewerColumn(_tableViewer, SWT.NONE);
		tableViewerColumn_3.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return _reductionMap.get(element);
			}
		});
		TableColumn tblclmnReduction = tableViewerColumn_3.getColumn();
		tblclmnReduction.setWidth(100);
		tblclmnReduction.setText("Reduction %");

		TableViewerColumn tableViewerColumn_4 = new TableViewerColumn(_tableViewer, SWT.NONE);
		tableViewerColumn_4.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				IResource res = (IResource) element;
				IPath path = res.getFullPath();
				return path.toString();
			}
		});
		TableColumn tblclmnFullPath = tableViewerColumn_4.getColumn();
		tblclmnFullPath.setWidth(328);
		tblclmnFullPath.setText("Full Path");
		_tableViewer.setContentProvider(new ArrayContentProvider());

		return container;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Optimize PNG Files");
	}

	@Override
	protected Control createContents(Composite parent) {
		Control contents = super.createContents(parent);
		getButton(OK).setEnabled(false);
		_labelTitle.setText("Running OptiPNG");
		try {
			List<IResource> list = new ArrayList<>();
			_oldSizeMap = new HashMap<>();
			_newSizeMap = new HashMap<>();
			_reductionMap = new HashMap<>();
			for (IResource res : _selection) {
				res.accept(new IResourceVisitor() {

					@Override
					public boolean visit(IResource resource) throws CoreException {
						if (resource.isDerived()) {
							out.println("OptiPNG Dialog: skip " + resource);
							return false;
						}

						if (OptiPNGCore.isPNG(res)) {
							if (!list.contains(resource)) {
								list.add(resource);
								_oldSizeMap.put(resource, getFileSize(resource));
							}
						}
						return true;
					}
				});
			}
			_tableViewer.setInput(list);

			WorkspaceJob job = new WorkspaceJob("Run OptiPNG on selection") {

				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
					try {
						monitor.beginTask("Optimizing PNGs", list.size());
						for (IResource resource : list) {

							Display.getDefault().asyncExec(new Runnable() {

								@Override
								public void run() {
									_labelTitle.setText("Optimizing " + resource.getName());
								}
							});

							IPath path = resource.getLocation();

							long len1 = path.toFile().length();

							out.println("OptiPNG Dialog: optimize " + path.toFile().getAbsolutePath());

							OptiPNGCore.optimize(path);
							OptiPNGCore.updateHashCache(resource);

							long len2 = path.toFile().length();

							int reduction = (int) (100 - (double) len2 / (double) len1 * 100);

							_reductionMap.put(resource, reduction + "%");
							_newSizeMap.put(resource, getFileSize(resource));

							monitor.worked(1);

							Display.getDefault().asyncExec(new Runnable() {

								@Override
								public void run() {
									_tableViewer.refresh(resource);
									_tableViewer.reveal(resource);
								}
							});
						}
						monitor.done();
						return Status.OK_STATUS;
					} catch (Exception e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					} finally {
						Display.getDefault().asyncExec(new Runnable() {

							@SuppressWarnings("synthetic-access")
							@Override
							public void run() {
								_labelTitle.setText("Done, all files are optimized.");
								getButton(OK).setEnabled(true);
							}
						});

						for (IResource res : _selection) {
							res.refreshLocal(IResource.DEPTH_INFINITE, monitor);
						}
					}
				}
			};
			job.schedule();
			return contents;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public void setSelection(List<IResource> selection) {
		_selection = selection;
	}

	/**
	 * Create contents of the button bar.
	 * 
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(811, 599);
	}

	static String getFileSize(IResource res) {
		return res.getLocation().toFile().length() / 1024 + "KB";
	}

}
