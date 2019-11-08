namespace phasereditor2d.ide.ui.actions {

    import controls = colibri.ui.controls;

    export class OpenNewWizardAction extends controls.Action {

        constructor() {
            super({
                text: "New",
                icon: IDEPlugin.getInstance().getIcon(ICON_NEW_FILE)
            });
        }

        run() {

            const viewer = new controls.viewers.TreeViewer();

            viewer.setLabelProvider(new WizardLabelProvider());
            viewer.setContentProvider(new controls.viewers.ArrayTreeContentProvider());
            viewer.setCellRendererProvider(new WizardCellRendererProvider());

            const extensions = colibri.ui.ide.Workbench.getWorkbench()
                .getExtensionRegistry()
                .getExtensions(phasereditor2d.ide.ui.wizards.NewWizardExtension.POINT);

            viewer.setInput(extensions);

            const dlg = new controls.dialogs.ViewerDialog(viewer);

            dlg.create();

            dlg.setTitle("New");

            {
                const selectCallback = () => {

                    dlg.close();

                    this.openFileDialog(viewer.getSelectionFirstElement());
                };

                const btn = dlg.addButton("Select", () => selectCallback());

                btn.disabled = true;

                viewer.addEventListener(controls.EVENT_SELECTION_CHANGED, e => {
                    btn.disabled = viewer.getSelection().length !== 1;
                });

                viewer.addEventListener(controls.viewers.EVENT_OPEN_ITEM, e => selectCallback());
            }

            dlg.addButton("Cancel", () => dlg.close());
        }

        private openFileDialog(extension : ide.ui.wizards.NewWizardExtension) {

            const dlg = new wizards.FileLocationDialog();

            dlg.create();

            dlg.setTitle(`New ${extension.getWizardName()}`);
            dlg.setInitialFileName(`${extension.getInitialFileName()}.${extension.getFileExtension()}`);
            dlg.setInitialLocation(extension.getInitialFileLocation());
        }
    }

    class WizardLabelProvider implements controls.viewers.ILabelProvider {

        getLabel(obj: any): string {
            return (obj as wizards.NewWizardExtension).getWizardName();
        }

    }

    class WizardCellRendererProvider implements controls.viewers.ICellRendererProvider {

        getCellRenderer(element: any): controls.viewers.ICellRenderer {

            const ext = element as wizards.NewWizardExtension;

            return new controls.viewers.IconImageCellRenderer(ext.getIcon());
        }

        preload(element: any): Promise<controls.PreloadResult> {
            return controls.Controls.resolveNothingLoaded();
        }

    }
}