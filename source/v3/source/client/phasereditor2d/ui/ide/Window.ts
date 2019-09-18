/// <reference path="../../../phasereditor2d.ui.controls/Control.ts" />

namespace phasereditor2d.ui.ide {

    export abstract class Window extends controls.Control {

        constructor() {
            super("div", "Window");

            this.setLayout(new controls.FillLayout(5));
        }

        protected createViewFolder(...parts : Part[]) : ViewFolder {

            const folder = new ViewFolder();
            for(const part of parts) {
                folder.addPart(part);
            }

            return folder;
        }

        abstract getEditorArea() : EditorArea;
    }
}