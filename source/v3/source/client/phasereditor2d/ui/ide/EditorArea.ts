/// <reference path="./Part.ts"/>
/// <reference path="./EditorPart.ts"/>
/// <reference path="./PartFolder.ts"/>
/// <reference path="../../../phasereditor2d.ui.controls/TabPane.ts"/>

namespace phasereditor2d.ui.ide {

    export class EditorArea extends PartFolder {

        constructor() {
            super("EditorArea");
        }

        activateEditor(editor : EditorPart) : void {
            super.selectTabWithContent(editor);
        }

    }
}