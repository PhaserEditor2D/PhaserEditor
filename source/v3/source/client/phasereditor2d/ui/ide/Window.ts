/// <reference path="../../../phasereditor2d.ui.controls/Control.ts" />

namespace phasereditor2d.ui.ide {

    export class Window extends controls.Control {
        constructor() {
            super("div", "Window");

            this.setLayout(new controls.FillLayout(5));
        }

        createViewFolder(...parts : Part[]) : ViewFolder {

            const folder = new ViewFolder();
            for(const part of parts) {
                folder.addPart(part);
            }

            return folder;

            // const tabPane = new controls.TabPane();

            // for(const part of parts) {
            //     tabPane.addTab(part.getTitle(), part);

            //     tabPane.addEventListener(controls.EVENT_CONTROL_LAYOUT, () => {
            //         part.layout();
            //     })
            // }

            // return tabPane;
        }
    }
}