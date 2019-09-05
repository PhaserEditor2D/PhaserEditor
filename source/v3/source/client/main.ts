namespace phasereditor2d {

    export async function main() {

        console.log("Preloading UI resources");
        await ui.controls.Controls.preload();

        console.log("Starting the workbench");
        const workbench = ui.ide.Workbench.getWorkbench();
        workbench.start();
    }
}

window.addEventListener("load", phasereditor2d.main);
