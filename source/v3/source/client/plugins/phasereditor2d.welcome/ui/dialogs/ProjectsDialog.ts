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

            const activeWindow = colibri.ui.ide.Workbench.getWorkbench().getActiveWindow();

            this.setCloseWithEscapeKey(!(activeWindow instanceof ui.WelcomeWindow));

            this.setTitle("Projects");

            this.addButton("New Project", () => { });

            {
                const btn = this.addButton("Open Project", () => this.openProject(viewer.getSelectionFirstElement()));

                btn.disabled = true;

                viewer.addEventListener(controls.EVENT_SELECTION_CHANGED, e => {
                    btn.disabled = !(viewer.getSelection().length === 1);
                });
            }

            let projects = await colibri.ui.ide.FileUtils.getProjects_async();

            const root = colibri.ui.ide.FileUtils.getRoot();

            if (root) {

                projects = projects.filter(project => root.getName() !== project);
            }

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