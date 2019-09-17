/// <reference path="./Part.ts"/>
/// <reference path="./EditorPart.ts"/>
/// <reference path="./PartFolder.ts"/>
/// <reference path="../../../phasereditor2d.ui.controls/TabPane.ts"/>

namespace phasereditor2d.ui.ide {

    class DemoEditor extends EditorPart {

        public constructor(id: string, title: string) {
            super(id);
            this.setTitle(title);
        }

        createPart(): void {
            this.getElement().innerHTML = "Editor " + this.getId();
        }
    }

    export class EditorArea extends PartFolder {

        public constructor() {
            super("EditorArea");

            //this.addPart(new DemoEditor("demoEditor1", "Level1.scene"), true);
            //this.addPart(new DemoEditor("demoEditor2", "Level2.scene"), true);
            //this.addPart(new DemoEditor("demoEditor3", "pack.json"), true);
        }

        public activateEditor(editor : EditorPart) : void {
            super.selectTabWithContent(editor);
        }

    }
}