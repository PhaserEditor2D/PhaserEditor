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
package phasereditor.ide.intro;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.ide.ChooseWorkspaceData;
import org.eclipse.ui.internal.ide.ChooseWorkspaceDialog;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.ide.StatusUtil;
import org.eclipse.ui.internal.ide.application.DelayedEventsProcessor;
import org.eclipse.ui.internal.ide.application.IDEApplication;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

import phasereditor.lic.LicCore;

/**
 * The "main program" for the Eclipse IDE.
 * 
 * This is a copy of the {@link IDEApplication} class, but with the unique
 * difference that it uses a {@link PhaserWorkbenchAdvisor} to start with the
 * Phaser perspective..
 * 
 * @since 3.0
 */
@SuppressWarnings("restriction")
public class PhaserIDE implements IApplication, IExecutableExtension {

	/**
	 * The name of the folder containing metadata information for the workspace.
	 */
	public static final String METADATA_FOLDER = ".metadata"; //$NON-NLS-1$

	private static final String VERSION_FILENAME = "version.ini"; //$NON-NLS-1$

	// Use the branding plug-in of the platform feature since this is most
	// likely
	// to change on an update of the IDE.
	private static final String WORKSPACE_CHECK_REFERENCE_BUNDLE_NAME = "org.eclipse.platform"; //$NON-NLS-1$
	private static final Version WORKSPACE_CHECK_REFERENCE_BUNDLE_VERSION;

	static {
		Bundle bundle = Platform.getBundle(WORKSPACE_CHECK_REFERENCE_BUNDLE_NAME);
		WORKSPACE_CHECK_REFERENCE_BUNDLE_VERSION = bundle != null ? bundle.getVersion()
				: null/* not installed */;
	}

	private static final String WORKSPACE_CHECK_REFERENCE_BUNDLE_NAME_LEGACY = "org.eclipse.core.runtime"; //$NON-NLS-1$
	private static final String WORKSPACE_CHECK_LEGACY_VERSION_INCREMENTED = "2"; //$NON-NLS-1$ legacy
	private static final String PROP_EXIT_CODE = "eclipse.exitcode"; //$NON-NLS-1$

	/**
	 * A special return code that will be recognized by the launcher and used to
	 * restart the workbench.
	 */
	private static final Integer EXIT_RELAUNCH = new Integer(24);

	/**
	 * A special return code that will be recognized by the PDE launcher and
	 * used to show an error dialog if the workspace is locked.
	 */
	private static final Integer EXIT_WORKSPACE_LOCKED = new Integer(15);

	/**
	 * The ID of the application plug-in
	 */
	public static final String PLUGIN_ID = "org.eclipse.ui.ide.application"; //$NON-NLS-1$

	/**
	 * Creates a new IDE application.
	 */
	public PhaserIDE() {
		// There is nothing to do for IDEApplication
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.
	 * IApplicationContext context)
	 */
	@Override
	public Object start(IApplicationContext appContext) throws Exception {
		javafx.application.Platform.setImplicitExit(false);

		LicCore.startEvaluationThread();

		Display display = createDisplay();

		// processor must be created before we start event loop
		DelayedEventsProcessor processor = new DelayedEventsProcessor(display);

		try {

			// look and see if there's a splash shell we can parent off of
			Shell shell = WorkbenchPlugin.getSplashShell(display);
			if (shell != null) {
				// should should set the icon and message for this shell to be
				// the
				// same as the chooser dialog - this will be the guy that lives
				// in
				// the task bar and without these calls you'd have the default
				// icon
				// with no message.
				shell.setText(ChooseWorkspaceDialog.getWindowTitle());
				shell.setImages(Window.getDefaultImages());
			}

			Object instanceLocationCheck = checkInstanceLocation(shell, appContext.getArguments());
			if (instanceLocationCheck != null) {
				WorkbenchPlugin.unsetSplashShell(display);
				appContext.applicationRunning();
				return instanceLocationCheck;
			}

			// create the workbench with this advisor and run it until it exits
			// N.B. createWorkbench remembers the advisor, and also registers
			// the workbench globally so that all UI plug-ins can find it using
			// PlatformUI.getWorkbench() or AbstractUIPlugin.getWorkbench()
			int returnCode = PlatformUI.createAndRunWorkbench(display, new PhaserWorkbenchAdvisor(processor));

			// the workbench doesn't support relaunch yet (bug 61809) so
			// for now restart is used, and exit data properties are checked
			// here to substitute in the relaunch return code if needed
			if (returnCode != PlatformUI.RETURN_RESTART) {
				return EXIT_OK;
			}

			// if the exit code property has been set to the relaunch code, then
			// return that code now, otherwise this is a normal restart
			return EXIT_RELAUNCH.equals(Integer.getInteger(PROP_EXIT_CODE)) ? EXIT_RELAUNCH : EXIT_RESTART;
		} finally {
			if (display != null) {
				display.dispose();
			}
			Location instanceLoc = Platform.getInstanceLocation();
			if (instanceLoc != null)
				instanceLoc.release();
		}
	}

	/**
	 * Creates the display used by the application.
	 * 
	 * @return the display used by the application
	 */
	protected static Display createDisplay() {
		return PlatformUI.createDisplay();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org
	 * .eclipse.core.runtime.IConfigurationElement, java.lang.String,
	 * java.lang.Object)
	 */
	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) {
		// There is nothing to do for IDEApplication
	}

	/**
	 * Return <code>null</code> if a valid workspace path has been set and an
	 * exit code otherwise. Prompt for and set the path if possible and
	 * required.
	 * 
	 * @param applicationArguments
	 *            the command line arguments
	 * @return <code>null</code> if a valid instance location has been set and
	 *         an exit code otherwise
	 */
	private static Object checkInstanceLocation(Shell shell, Map<?, ?> applicationArguments) {
		// -data @none was specified but an ide requires workspace
		Location instanceLoc = Platform.getInstanceLocation();
		if (instanceLoc == null) {
			MessageDialog.openError(shell, IDEWorkbenchMessages.IDEApplication_workspaceMandatoryTitle,
					IDEWorkbenchMessages.IDEApplication_workspaceMandatoryMessage);
			return EXIT_OK;
		}

		// -data "/valid/path", workspace already set
		if (instanceLoc.isSet()) {
			// make sure the meta data version is compatible (or the user has
			// chosen to overwrite it).
			if (!checkValidWorkspace(shell, instanceLoc.getURL())) {
				return EXIT_OK;
			}

			// at this point its valid, so try to lock it and update the
			// metadata version information if successful
			try {
				if (instanceLoc.lock()) {
					writeWorkspaceVersion();
					return null;
				}

				// we failed to create the directory.
				// Two possibilities:
				// 1. directory is already in use
				// 2. directory could not be created
				File workspaceDirectory = new File(instanceLoc.getURL().getFile());
				if (workspaceDirectory.exists()) {
					if (isDevLaunchMode(applicationArguments)) {
						return EXIT_WORKSPACE_LOCKED;
					}
					MessageDialog.openError(shell, IDEWorkbenchMessages.IDEApplication_workspaceCannotLockTitle,
							NLS.bind(IDEWorkbenchMessages.IDEApplication_workspaceCannotLockMessage,
									workspaceDirectory.getAbsolutePath()));
				} else {
					MessageDialog.openError(shell, IDEWorkbenchMessages.IDEApplication_workspaceCannotBeSetTitle,
							IDEWorkbenchMessages.IDEApplication_workspaceCannotBeSetMessage);
				}
			} catch (IOException e) {
				IDEWorkbenchPlugin.log("Could not obtain lock for workspace location", //$NON-NLS-1$
						e);
				MessageDialog.openError(shell, IDEWorkbenchMessages.InternalError, e.getMessage());
			}
			return EXIT_OK;
		}

		// -data @noDefault or -data not specified, prompt and set
		ChooseWorkspaceData launchData = new ChooseWorkspaceData(instanceLoc.getDefault());

		boolean force = false;
		while (true) {
			URL workspaceUrl = promptForWorkspace(shell, launchData, force);
			if (workspaceUrl == null) {
				return EXIT_OK;
			}

			// if there is an error with the first selection, then force the
			// dialog to open to give the user a chance to correct
			force = true;

			try {
				// the operation will fail if the url is not a valid
				// instance data area, so other checking is unneeded
				if (instanceLoc.set(workspaceUrl, true)) {
					launchData.writePersistedData();
					writeWorkspaceVersion();
					return null;
				}
			} catch (IllegalStateException e) {
				MessageDialog.openError(shell, IDEWorkbenchMessages.IDEApplication_workspaceCannotBeSetTitle,
						IDEWorkbenchMessages.IDEApplication_workspaceCannotBeSetMessage);
				return EXIT_OK;
			} catch (IOException e) {
				MessageDialog.openError(shell, IDEWorkbenchMessages.IDEApplication_workspaceCannotBeSetTitle,
						IDEWorkbenchMessages.IDEApplication_workspaceCannotBeSetMessage);
			}

			// by this point it has been determined that the workspace is
			// already in use -- force the user to choose again
			MessageDialog.openError(shell, IDEWorkbenchMessages.IDEApplication_workspaceInUseTitle,
					NLS.bind(IDEWorkbenchMessages.IDEApplication_workspaceInUseMessage, workspaceUrl.getFile()));
		}
	}

	private static boolean isDevLaunchMode(Map<?, ?> args) {
		// see org.eclipse.pde.internal.core.PluginPathFinder.isDevLaunchMode()
		if (Boolean.getBoolean("eclipse.pde.launch")) //$NON-NLS-1$
			return true;
		return args.containsKey("-pdelaunch"); //$NON-NLS-1$
	}

	/**
	 * Open a workspace selection dialog on the argument shell, populating the
	 * argument data with the user's selection. Perform first level validation
	 * on the selection by comparing the version information. This method does
	 * not examine the runtime state (e.g., is the workspace already locked?).
	 * 
	 * @param shell
	 * @param launchData
	 * @param force
	 *            setting to true makes the dialog open regardless of the
	 *            showDialog value
	 * @return An URL storing the selected workspace or null if the user has
	 *         canceled the launch operation.
	 */
	private static URL promptForWorkspace(Shell shell, ChooseWorkspaceData launchData, boolean force) {
		URL url = null;
		do {
			// okay to use the shell now - this is the splash shell
			new ChooseWorkspaceDialog(shell, launchData, false, true) {
				@Override
				protected Shell getParentShell() {
					return null;
				}
			}.prompt(force);
			String instancePath = launchData.getSelection();
			if (instancePath == null) {
				return null;
			}

			// the dialog is not forced on the first iteration, but is on every
			// subsequent one -- if there was an error then the user needs to be
			// allowed to fix it
			// force = true;

			// 70576: don't accept empty input
			if (instancePath.length() <= 0) {
				MessageDialog.openError(shell, IDEWorkbenchMessages.IDEApplication_workspaceEmptyTitle,
						IDEWorkbenchMessages.IDEApplication_workspaceEmptyMessage);
				continue;
			}

			// create the workspace if it does not already exist
			File workspace = new File(instancePath);
			if (!workspace.exists()) {
				workspace.mkdir();
			}

			try {
				// Don't use File.toURL() since it adds a leading slash that
				// Platform does not
				// handle properly. See bug 54081 for more details.
				String path = workspace.getAbsolutePath().replace(File.separatorChar, '/');
				url = new URL("file", null, path); //$NON-NLS-1$
			} catch (MalformedURLException e) {
				MessageDialog.openError(shell, IDEWorkbenchMessages.IDEApplication_workspaceInvalidTitle,
						IDEWorkbenchMessages.IDEApplication_workspaceInvalidMessage);
				continue;
			}
		} while (!checkValidWorkspace(shell, url));

		return url;
	}

	/**
	 * Return true if the argument directory is ok to use as a workspace and
	 * false otherwise. A version check will be performed, and a confirmation
	 * box may be displayed on the argument shell if an older version is
	 * detected.
	 * 
	 * @return true if the argument URL is ok to use as a workspace and false
	 *         otherwise.
	 */
	private static boolean checkValidWorkspace(Shell shell, URL url) {
		// a null url is not a valid workspace
		if (url == null) {
			return false;
		}

		if (WORKSPACE_CHECK_REFERENCE_BUNDLE_VERSION == null) {
			// no reference bundle installed, no check possible
			return true;
		}

		Version version = readWorkspaceVersion(url);
		// if the version could not be read, then there is not any existing
		// workspace data to trample, e.g., perhaps its a new directory that
		// is just starting to be used as a workspace
		if (version == null) {
			return true;
		}

		final Version ide_version = toMajorMinorVersion(WORKSPACE_CHECK_REFERENCE_BUNDLE_VERSION);
		Version workspace_version = toMajorMinorVersion(version);
		int versionCompareResult = workspace_version.compareTo(ide_version);

		// equality test is required since any version difference (newer
		// or older) may result in data being trampled
		if (versionCompareResult == 0) {
			return true;
		}

		// At this point workspace has been detected to be from a version
		// other than the current ide version -- find out if the user wants
		// to use it anyhow.
		int severity;
		String title;
		String message;
		if (versionCompareResult < 0) {
			// Workspace < IDE. Update must be possible without issues,
			// so only inform user about it.
			severity = MessageDialog.INFORMATION;
			title = IDEWorkbenchMessages.IDEApplication_versionTitle_olderWorkspace;
			message = NLS.bind(IDEWorkbenchMessages.IDEApplication_versionMessage_olderWorkspace, url.getFile());
		} else {
			// Workspace > IDE. It must have been opened with a newer IDE
			// version.
			// Downgrade might be problematic, so warn user about it.
			severity = MessageDialog.WARNING;
			title = IDEWorkbenchMessages.IDEApplication_versionTitle_newerWorkspace;
			message = NLS.bind(IDEWorkbenchMessages.IDEApplication_versionMessage_newerWorkspace, url.getFile());
		}

		MessageDialog dialog = new MessageDialog(shell, title, null, message, severity,
				new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL }, 0);
		return dialog.open() == Window.OK;
	}

	/**
	 * Look at the argument URL for the workspace's version information. Return
	 * that version if found and null otherwise.
	 */
	private static Version readWorkspaceVersion(URL workspace) {
		File versionFile = getVersionFile(workspace, false);
		if (versionFile == null || !versionFile.exists()) {
			return null;
		}

		try {
			// Although the version file is not spec'ed to be a Java properties
			// file, it happens to follow the same format currently, so using
			// Properties to read it is convenient.
			Properties props = new Properties();

			try (FileInputStream is = new FileInputStream(versionFile)) {
				props.load(is);
			}

			String versionString = props.getProperty(WORKSPACE_CHECK_REFERENCE_BUNDLE_NAME);
			if (versionString != null) {
				return Version.parseVersion(versionString);
			}
			versionString = props.getProperty(WORKSPACE_CHECK_REFERENCE_BUNDLE_NAME_LEGACY);
			if (versionString != null) {
				return Version.parseVersion(versionString);
			}
			return null;
		} catch (IOException e) {
			IDEWorkbenchPlugin.log("Could not read version file " + versionFile, //$NON-NLS-1$
					new Status(IStatus.ERROR, IDEWorkbenchPlugin.IDE_WORKBENCH, IStatus.ERROR,
							e.getMessage() == null ? "" : e.getMessage(), //$NON-NLS-1$
							e));
			return null;
		} catch (IllegalArgumentException e) {
			IDEWorkbenchPlugin.log("Could not parse version in " + versionFile, //$NON-NLS-1$
					new Status(IStatus.ERROR, IDEWorkbenchPlugin.IDE_WORKBENCH, IStatus.ERROR,
							e.getMessage() == null ? "" : e.getMessage(), //$NON-NLS-1$
							e));
			return null;
		}
	}

	/**
	 * Write the version of the metadata into a known file overwriting any
	 * existing file contents. Writing the version file isn't really crucial, so
	 * the function is silent about failure
	 */
	private static void writeWorkspaceVersion() {
		if (WORKSPACE_CHECK_REFERENCE_BUNDLE_VERSION == null) {
			// no reference bundle installed, no check possible
			return;
		}

		Location instanceLoc = Platform.getInstanceLocation();
		if (instanceLoc == null || instanceLoc.isReadOnly()) {
			return;
		}

		File versionFile = getVersionFile(instanceLoc.getURL(), true);
		if (versionFile == null) {
			return;
		}

		try (OutputStream output = new FileOutputStream(versionFile)) {
			Properties props = new Properties();

			// write new property
			props.setProperty(WORKSPACE_CHECK_REFERENCE_BUNDLE_NAME,
					WORKSPACE_CHECK_REFERENCE_BUNDLE_VERSION.toString());

			// write legacy property with an incremented version,
			// so that pre-4.4 IDEs will also warn about the workspace
			props.setProperty(WORKSPACE_CHECK_REFERENCE_BUNDLE_NAME_LEGACY, WORKSPACE_CHECK_LEGACY_VERSION_INCREMENTED);

			props.store(output, null);
		} catch (IOException e) {
			IDEWorkbenchPlugin.log("Could not write version file", //$NON-NLS-1$
					StatusUtil.newStatus(IStatus.ERROR, e.getMessage(), e));
		}
	}

	/**
	 * The version file is stored in the metadata area of the workspace. This
	 * method returns an URL to the file or null if the directory or file does
	 * not exist (and the create parameter is false).
	 * 
	 * @param create
	 *            If the directory and file does not exist this parameter
	 *            controls whether it will be created.
	 * @return An url to the file or null if the version file does not exist or
	 *         could not be created.
	 */
	private static File getVersionFile(URL workspaceUrl, boolean create) {
		if (workspaceUrl == null) {
			return null;
		}

		try {
			// make sure the directory exists
			File metaDir = new File(workspaceUrl.getPath(), METADATA_FOLDER);
			if (!metaDir.exists() && (!create || !metaDir.mkdir())) {
				return null;
			}

			// make sure the file exists
			File versionFile = new File(metaDir, VERSION_FILENAME);
			if (!versionFile.exists() && (!create || !versionFile.createNewFile())) {
				return null;
			}

			return versionFile;
		} catch (IOException e) {
			// cannot log because instance area has not been set
			return null;
		}
	}

	/**
	 * @return the major and minor parts of the given version
	 */
	private static Version toMajorMinorVersion(Version version) {
		return new Version(version.getMajor(), version.getMinor(), 0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.app.IApplication#stop()
	 */
	@Override
	public void stop() {
		final IWorkbench workbench = PlatformUI.getWorkbench();
		if (workbench == null)
			return;
		final Display display = workbench.getDisplay();
		display.syncExec(new Runnable() {
			@Override
			public void run() {
				if (!display.isDisposed())
					workbench.close();
			}
		});
		javafx.application.Platform.exit();
	}
}
