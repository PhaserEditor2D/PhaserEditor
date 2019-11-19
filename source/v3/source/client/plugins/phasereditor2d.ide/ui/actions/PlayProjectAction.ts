namespace phasereditor2d.ide.ui.actions {

    import controls = colibri.ui.controls;

    export class PlayProjectAction extends controls.Action {

        constructor() {
            super({
                text: "Play Project",
                icon: IDEPlugin.getInstance().getIcon(ICON_PLAY)
            });
        }

        run() {

            const element = document.createElement("a");

            element.href = colibri.ui.ide.FileUtils.getRoot().getUrl();
            element.target = "blank";

            document.body.append(element);

            element.click();

            element.remove();
        }
    }
}