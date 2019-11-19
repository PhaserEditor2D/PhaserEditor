namespace phasereditor2d.welcome.ui.actions {

    import controls = colibri.ui.controls;

    export class OpenProjectsDialogAction extends controls.Action {


        constructor() {
            super({
                text: "Projects",
                icon: colibri.ui.ide.Workbench.getWorkbench().getWorkbenchIcon(colibri.ui.ide.ICON_FOLDER)
            });
        }

        run() {

            const dlg = new dialogs.ProjectsDialog();

            dlg.create();

            dlg.addButton("Cancel", () => dlg.close());
        }
    }
}