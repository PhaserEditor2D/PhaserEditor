namespace phasereditor2d.ui.ide.commands {

    export class CommandArgs {
        constructor(
            public readonly activePart : Part,
            public readonly activeEditor : EditorPart,
            public readonly activeElement: HTMLElement
        ) {

        }
    }

}