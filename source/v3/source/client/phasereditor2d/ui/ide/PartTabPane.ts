/// <reference path="../../../phasereditor2d.ui.controls/TabPane.ts" />

namespace phasereditor2d.ui.ide {

    export class PartTabPane extends controls.TabPane {

        public constructor(...classList: string[]) {
            super("PartsTabPane", ...classList);

            this.addEventListener(controls.EVENT_TAB_CLOSED, (e : CustomEvent) => {
                const part = <Part> e.detail;
                part.onPartClosed();
            });

            this.addEventListener(controls.EVENT_TAB_SELECTED, (e : CustomEvent) => {
                const part = <Part> e.detail;
                part.onPartActivated();
            });
        }

        public addPart(part : Part, closeable  = false) : void {
            this.addTab(part.getTitle(), part, closeable);
        }
    }

}