/// <reference path="./Part.ts"/>
/// <reference path="../../../phasereditor2d.ui.controls/TabPane.ts"/>

namespace phasereditor2d.ui.ide {

    export class EditorArea extends controls.TabPane {

        constructor() {
            super("EditorArea");

            this.createTest("Level 1.scene");
            this.createTest("pack.json");
            this.createTest("Level 3.scene");
        }

        private createTest(label: string): void {
            this.addTab(label, () => {
                const content = new controls.Control("div");
                content.getElement().innerHTML = `<p>Hello I am a content</p><p>For ${label}</p>`;
                return content;
            });
        }
    }
}