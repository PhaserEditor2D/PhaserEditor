namespace phasereditor2d {


    export const VER = "3.0.0";

    export async function main() {

        console.log(`%c %c Phaser Editor 2D %c v${VER} %c %c https://phasereditor2d.com `,
            "background-color:red",
            "background-color:#3f3f3f;color:whitesmoke",
            "background-color:orange;color:black",
            "background-color:red",
            "background-color:silver",
        );

        await colibri.ui.ide.Workbench.getWorkbench()

            .launch([

                ui.ide.editors.scene.SceneEditorPlugin.getInstance(),

                ui.ide.editors.pack.AssetPackEditorPlugin.getInstance(),

                ui.ide.DesignPlugin.getInstance()

            ]);
    }
}

window.addEventListener("load", phasereditor2d.main);
