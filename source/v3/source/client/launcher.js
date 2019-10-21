
const VER = "3.0.0";

function main() {

    console.log(`%c %c Phaser Editor 2D %c v${VER} %c %c https://phasereditor2d.com `,
        "background-color:red",
        "background-color:#3f3f3f;color:whitesmoke",
        "background-color:orange;color:black",
        "background-color:red",
        "background-color:silver",
    );

    colibri.ui.ide.Workbench.getWorkbench()

        .launch(

            // [

            //     phasereditor2d.phaser.PhaserPlugin.getInstance(),

            //     phasereditor2d.inspector.InspectorPlugin.getInstance(),

            //     phasereditor2d.outline.OutlinePlugin.getInstance(),

            //     phasereditor2d.blocks.BlocksPlugin.getInstance(),

            //     phasereditor2d.images.ImagesPlugin.getInstance(),

            //     phasereditor2d.pack.AssetPackPlugin.getInstance(),

            //     phasereditor2d.scene.ScenePlugin.getInstance(),

            //     phasereditor2d.files.FilesPlugin.getInstance(),

            //     phasereditor2d.ide.IDEPlugin.getInstance()

            // ]

        );
}


window.addEventListener("load", main);