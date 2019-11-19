namespace phasereditor2d.welcome.ui {

    import controls = colibri.ui.controls;

    export class WelcomeWindow extends colibri.ui.ide.WorkbenchWindow {

        static ID = "phasereditor2d.welcome.ui.WelcomeWindow";

        constructor() {
            super(WelcomeWindow.ID);
        }

        getEditorArea(): colibri.ui.ide.EditorArea {
            return new colibri.ui.ide.EditorArea();
        }

        protected async createParts() {
        
            const dlg = new dialogs.ProjectsDialog();

            dlg.create();

            dlg.setCloseWithEscapeKey(false);
        }
    }
}