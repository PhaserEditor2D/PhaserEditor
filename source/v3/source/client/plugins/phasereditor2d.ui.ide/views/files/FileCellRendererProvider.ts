namespace phasereditor2d.ui.ide.views.files {

    import io = phasereditor2d.core.io;
    import viewers = phasereditor2d.ui.controls.viewers;

    export class FileCellRendererProvider implements viewers.ICellRendererProvider {

        getCellRenderer(file: io.FilePath): viewers.ICellRenderer {

            const contentType = Workbench.getWorkbench().getContentTypeRegistry().getCachedContentType(file);

            switch (contentType) {

                case ui.ide.CONTENT_TYPE_IMAGE:
                    return new FileImageRenderer();

                case ui.ide.editors.scene.CONTENT_TYPE_SCENE:
                    return new ui.ide.editors.scene.blocks.SceneCellRenderer();
            }

            return new FileCellRenderer();
        }

        preload(file: io.FilePath): Promise<controls.PreloadResult> {
            return Workbench.getWorkbench().getContentTypeRegistry().preload(file);
        }
    }
}