namespace phasereditor2d.welcome.ui.dialogs {

    import controls = colibri.ui.controls;

    export class ProjectsDialog extends controls.dialogs.ViewerDialog {

        constructor() {
            super(new controls.viewers.TreeViewer());


        }

        async create() {

            super.create();

            const viewer = this.getViewer();

            viewer.setLabelProvider(new controls.viewers.LabelProvider());
            viewer.setCellRendererProvider(new viewers.ProjectCellRendererProvider());
            viewer.setContentProvider(new controls.viewers.ArrayTreeContentProvider());
            viewer.setInput([]);

            this.setCloseWithEscapeKey(false);

            this.setTitle("Projects");

            this.addButton("New Project", () => { });

            {
                const btn = this.addButton("Open Project", () => this.openProject(viewer.getSelectionFirstElement()));

                btn.disabled = true;

                viewer.addEventListener(controls.EVENT_SELECTION_CHANGED, e => {
                    btn.disabled = !(viewer.getSelection().length === 1);
                });
            }

            const projects = await colibri.ui.ide.FileUtils.getProjects_async().then();

            viewer.setInput(projects);
            viewer.repaint();
        }

        private async openProject(project: string) {

            const wb = colibri.ui.ide.Workbench.getWorkbench();

            await wb.openProject(project);

            this.close();

            wb.activateWindow(ide.ui.DesignWindow.ID);
        }
    }
}