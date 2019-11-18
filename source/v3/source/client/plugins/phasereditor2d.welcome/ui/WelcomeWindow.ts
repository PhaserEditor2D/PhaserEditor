namespace phasereditor2d.welcome.ui {

    import controls = colibri.ui.controls;

    export class WelcomeWindow extends colibri.ui.ide.WorkbenchWindow {

        constructor() {
            super("phasereditor2d.welcome.ui.WelcomeWindow");
        }

        getEditorArea(): colibri.ui.ide.EditorArea {
            return new colibri.ui.ide.EditorArea();
        }

        protected createParts() {

            const dlg = new controls.dialogs.Dialog("WelcomeDialog");

            dlg.create();

            dlg.setTitle("Projects");

            dlg.addButton("Open Project", () => { });
            dlg.addButton("New Project", () => { });
        }
    }
}