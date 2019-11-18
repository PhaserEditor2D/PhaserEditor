namespace phasereditor2d.welcome.ui {

    import controls = colibri.ui.controls;

    export class WelcomeWindow extends colibri.ui.ide.WorkbenchWindow {

        constructor() {
            super("phasereditor2d.welcome.ui.WelcomeWindow");
        }

        getEditorArea(): colibri.ui.ide.EditorArea {
            return new colibri.ui.ide.EditorArea();
        }

        protected async createParts() {
        
            const dlg = new dialogs.ProjectsDialog();

            dlg.create();
        }
    }
}