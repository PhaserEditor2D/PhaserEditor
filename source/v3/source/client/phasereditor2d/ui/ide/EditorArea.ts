/// <reference path="./Part.ts"/>
/// <reference path="./EditorPart.ts"/>
/// <reference path="../../../phasereditor2d.ui.controls/TabPane.ts"/>

namespace phasereditor2d.ui.ide {

    class DemoEditor extends EditorPart {

        constructor(id: string) {
            super(id);
        }
    }

    export class EditorArea extends controls.TabPane {

        constructor() {
            super("EditorArea");
            this.addTab("Level 1.scene", () => new DemoEditor("demoEditor1"));
            this.addTab("Level 2.scene", () => new DemoEditor("demoEditor2"));
            this.addTab("pack.json", () => new DemoEditor("demoEditor3"));
        }

    }
}