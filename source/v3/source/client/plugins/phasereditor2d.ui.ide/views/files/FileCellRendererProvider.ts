namespace phasereditor2d.ui.ide.views.files {

    import controls = colibri.ui.controls;
    import ide = colibri.ui.ide;
    import core = colibri.core;
    import io = colibri.core.io;
    import viewers = colibri.ui.controls.viewers;

    export class FileCellRendererProvider implements viewers.ICellRendererProvider {

        getCellRenderer(file: io.FilePath): viewers.ICellRenderer {

            const contentType = ide.Workbench.getWorkbench().getContentTypeRegistry().getCachedContentType(file);

            switch (contentType) {

                case ide.CONTENT_TYPE_IMAGE:
                    return new FileImageRenderer();

                case ui.ide.editors.scene.CONTENT_TYPE_SCENE:
                    return new ui.ide.editors.scene.blocks.SceneCellRenderer();
            }

            return new FileCellRenderer();
        }

        preload(file: io.FilePath): Promise<controls.PreloadResult> {
            return ide.Workbench.getWorkbench().getContentTypeRegistry().preload(file);
        }
    }
}