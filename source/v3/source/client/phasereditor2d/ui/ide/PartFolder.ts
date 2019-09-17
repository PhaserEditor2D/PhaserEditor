/// <reference path="../../../phasereditor2d.ui.controls/TabPane.ts" />

namespace phasereditor2d.ui.ide {

    export class PartFolder extends controls.TabPane {

        public constructor(...classList: string[]) {
            super("PartsTabPane", ...classList);

            this.addEventListener(controls.EVENT_CONTROL_LAYOUT, (e: CustomEvent) => {
                const content = this.getSelectedTabContent();
                if (content) {
                    content.layout();
                }
            })

            this.addEventListener(controls.EVENT_TAB_CLOSED, (e: CustomEvent) => {
                const part = <Part>e.detail;
                part.onPartClosed();
            });

            this.addEventListener(controls.EVENT_TAB_SELECTED, (e: CustomEvent) => {
                const part = <Part>e.detail;
                part.onPartShown();
            });
        }

        public addPart(part: Part, closeable = false): void {
            part.addEventListener(EVENT_PART_TITLE_UPDATED, (e: CustomEvent) => {
                this.setTabTitle(part, part.getTitle());
            });
            this.addTab(part.getTitle(), part, closeable);
        }
    }

}