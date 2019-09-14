namespace phasereditor2d.ui.ide {

    export class Window extends controls.Control {
        constructor() {
            super("div", "Window");

            this.setLayout(new controls.FillLayout(5));
        }

        createPartFolder(...parts : Part[]) : controls.TabPane {
            const tabPane = new controls.TabPane("WorkbenchFolder", "PartFolder");

            for(const part of parts) {
                tabPane.addTab(part.getTitle(), () => part);

                tabPane.addEventListener(controls.CONTROL_LAYOUT_EVENT, () => {
                    part.layout();
                })
            }

            return tabPane;
        }
    }
}