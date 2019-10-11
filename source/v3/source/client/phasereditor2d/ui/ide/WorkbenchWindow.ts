/// <reference path="../../../phasereditor2d.ui.controls/Control.ts" />

namespace phasereditor2d.ui.ide {

    export abstract class WorkbenchWindow extends controls.Control {

        constructor() {
            super("div", "Window");

            this.setLayout(new controls.FillLayout(5));

            window.addEventListener("resize", e => {
                this.setBoundsValues(0, 0, window.innerWidth, window.innerHeight);
            });

            window.addEventListener(controls.EVENT_THEME_CHANGED, e => this.layout());
        }

        protected createViewFolder(...parts: Part[]): ViewFolder {

            const folder = new ViewFolder();
            for (const part of parts) {
                folder.addPart(part);
            }

            return folder;
        }

        abstract getEditorArea(): EditorArea;
    }
}